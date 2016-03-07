package main

import (
	log "github.com/Sirupsen/logrus"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/awserr"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"

	"net/http"

	. "time"

	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"strconv"
	"strings"
    "math"
    "sync"
)

type ArchiveClient struct {
	BucketName string
	client     *s3.S3
}

func (g *ArchiveClient) PollAndUpload(t Time) error {

	err, file := g.getFromArchive(t)
	if err != nil {
		return err
	}

	params := &s3.PutObjectInput{
		Bucket:          aws.String(g.BucketName), // required
		Key:             aws.String(g.S3Path(t)),  // required
		ACL:             aws.String("public-read"),
		Body:            file,
		ContentLength:   aws.Int64(int64(file.Len())),
		ContentType:     aws.String("application/json"),
		ContentEncoding: aws.String("gzip"),
		// see more at http://godoc.org/github.com/aws/aws-sdk-go/service/s3#S3.PutObject
	}

	_, err = g.client.PutObject(params)

	if err != nil {
		if awsErr, ok := err.(awserr.Error); ok {
			// Generic AWS Error with Code, Message, and original error (if any)
			log.Error(awsErr.Code(), awsErr.Message(), awsErr.OrigErr())
			if reqErr, ok := err.(awserr.RequestFailure); ok {
				// A service error occurred
				log.Error(reqErr.Code(), reqErr.Message(), reqErr.StatusCode(), reqErr.RequestID())
				return reqErr
			}
		} else {
			// This case should never be hit, the SDK should always return an
			// error which satisfies the awserr.Error interface.
			return err
		}
	}
	return nil
}

func New(bucket string) (*ArchiveClient, error) {
	creds := credentials.NewSharedCredentials(os.Getenv("HOME")+"/.aws/credentials", "default")
	_, err := creds.Get()

	if err != nil {
		return nil, err
	}

	config := &aws.Config{
		Region:           aws.String("us-east-1"),
		Endpoint:         aws.String(""),
		S3ForcePathStyle: aws.Bool(true),
		Credentials:      creds,
	}

	s3client := s3.New(session.New(config))

	return &ArchiveClient{
		BucketName: bucket,
		client:     s3client,
	}, nil
}

// Same as PollAndUpload but checks if file exists in S3 first.
func (g *ArchiveClient) CheckPollAndUpload(t Time) error {
	exists, err := g.DataExists(t)
	filepath := g.BucketName + g.S3Path(t)

	if err != nil {
		return err
	}

	if exists {
		log.Info("File already exists in S3: " + filepath)
	} else {
		log.Info("File doesn't exist, time to upload: " + filepath)
		err = g.PollAndUpload(t)

		if err != nil {
			return err
		} else {
			log.Info("File successfully uploaded: " + filepath)
		}
	}
	return nil
}

func (g *ArchiveClient) UploadWithRetries(t Time, retries int) error {
	retryNum := 0
	var err error
	for retryNum < retries {
		err = g.CheckPollAndUpload(t.Add(-delay))
		if err == nil {
			return nil
		}
		retryNum++
	}
	return err
}

func intervals(start Time, end Time, delaySec Duration, stepSec Duration) []Time {
	startShifted := start.Add(-delaySec)
	endShifted := end.Add(-delaySec)

	var intervals []Time
	for startShifted.Before(endShifted) {
		intervals = append(intervals, startShifted)
		startShifted = startShifted.Add(stepSec)
	}
	return intervals
}

func (g *ArchiveClient) BackFill(start Time, end Time, delaySec Duration, stepSec Duration, retries int, parallelism int) []Time {
	intervals := intervals(start, end, delaySec, stepSec)

	intervalChan := make(chan Time, len(intervals))
	failedIntervalsChan := make(chan Time, len(intervals))

	minParallelism := int(math.Min(float64(parallelism), float64(len(intervals))))

	for _, interval := range intervals {
		intervalChan <- interval
	}
	close(intervalChan)

	var wg sync.WaitGroup
	wg.Add(minParallelism)
	for i := 0; i < minParallelism; i++ {
		go func(intervals <-chan Time, failedIntervals chan<- Time) {
			for interval := range intervals {
				err := g.UploadWithRetries(interval, retries)
				if err != nil {
					failedIntervalsChan <- interval
				}
			}
			wg.Done()
		}(intervalChan, failedIntervalsChan)
	}
	wg.Wait()
	close(failedIntervalsChan)

	var failedIntervals []Time
	for interval := range failedIntervalsChan {
		failedIntervals = append(failedIntervals, interval)
	}

	return failedIntervals
}

// Consistent check to see if archive data exists in S3 for a given time period.
// Does an ACL check which is not eventually consistent, as opposed to a listing.
func (g *ArchiveClient) DataExists(time Time) (bool, error) {
	_, err := g.client.HeadObject(&s3.HeadObjectInput{
		Bucket: aws.String(g.BucketName),
		Key:    aws.String(g.S3Path(time)),
	})

	if err != nil {
		if strings.Contains(err.Error(), "status code: 404") {
			return false, nil
		}
		return false, err
	}

	return true, nil
}

func (g *ArchiveClient) S3Path(t Time) string {
	return t.Format("/2006/01/02/") + strconv.Itoa(t.Hour()) + (".json.gz")
}

func (g *ArchiveClient) ArchivePath(t Time) string {
	return t.Format("2006-01-02-") + strconv.Itoa(t.Hour()) + ".json.gz"
}

type HttpError struct {
	Status     string
	StatusCode int
	Url        string
}

func (e *HttpError) Error() string {
	return fmt.Sprintf("%s: %s", e.Url, e.Status)
}

// Retrieves file from githubarchive.org and writes it to disk, returning a file pointer.
// Make sure to call .Close() on the returned file. You'll probably want to delete
// the file once its pushed to S3 as well.
func (g *ArchiveClient) getFromArchive(time Time) (error, *bytes.Reader) {
	url := "http://data.githubarchive.org/" + g.ArchivePath(time)

	response, err := http.Get(url)

	if err != nil {
		return err, nil
	} else if response.StatusCode == 404 {
		return &HttpError{"Data does not exist", 404, url}, nil
	} else if response.StatusCode != 200 {
		return &HttpError{"Unexpected Status", response.StatusCode, url}, nil
	}
	defer response.Body.Close()

	data, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return err, nil
	}

	reader := bytes.NewReader(data)
	return nil, reader
}