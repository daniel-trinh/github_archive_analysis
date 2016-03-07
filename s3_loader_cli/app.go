package main

import (
	log "github.com/Sirupsen/logrus"
	"github.com/spf13/cobra"

	"fmt"
	"os"
	"time"
	. "time"
)

var RootCmd = &cobra.Command{
	Use:   "s3_loader_cli",
	Short: "Load github data to S3",
	Long: `Load data from http://githubarchive.org/ to S3. Can be used to backfill missing time, or as a continuously polling agent.`,
}
var backfillCmd = &cobra.Command{
	Use:   "backfill",
	Short: "Backfill missing data to S3",
	Long:  "Run the program to backfill missing data. By default will not reupload if data already exists in S3.",
}

var daemonCmd = &cobra.Command{
	Use:   "daemon",
	Short: "Continuously poll and upload to S3",
	Long:  "Acts as a polling daemon to the archive, uploading to S3 periodically.",
}

var startSkew, endSkew, delay, period Duration
var parallelism, retries int

func backfill(cmd *cobra.Command, args []string) {
	c, err := New("github-archive-data")
	if err != nil {
		log.Error(err.Error())
		os.Exit(-1)
	}

	t := time.Now()

	start := t.Add(-startSkew)
	end := t.Add(-endSkew)

	if start.After(end) {
		fmt.Println("end_skew must be less than start_skew")
		os.Exit(-1)
	}

	failedIntervals := c.BackFill(start, end, delay, Hour, retries, parallelism)

	if len(failedIntervals) > 0 {
		fmt.Println("Failed intervals:")
		fmt.Println(failedIntervals)
	}
}

func daemon(cmd *cobra.Command, args []string) {
	c, err := New("s3_loader_cli")
	if err != nil {
		log.Error(err.Error())
		os.Exit(-1)
	}

	ticker := time.NewTicker(time.Minute * 30)
	timer := time.NewTimer(0)

	for {
		select {
		case tick := <-ticker.C:
			c.UploadWithRetries(tick, retries)
		case time := <-timer.C:
			c.UploadWithRetries(time, retries)
		}
	}
}

func init() {

	backfillCmd.Run = backfill
	daemonCmd.Run = daemon
	RootCmd.AddCommand(backfillCmd, daemonCmd)

	backfillCmd.Flags().DurationVarP(&startSkew,
		"start_skew",
		"s",
		0,
		"If present, will subtract this duration from the current time as a starting point for backfilling data. The final calculated time (time.Now() - StartFromPresent) must be before (time.Now() - EndFromPresent)",
	)
	backfillCmd.Flags().DurationVarP(&endSkew,
		"end_skew",
		"e",
		0,
		"If present, will subtract this duration from the current time as the ending point for backfilling data. The final calculated time (time.Now() - EndFromPresent) must be after (time.Now() - StartFromPresent).",
	)
	backfillCmd.Flags().IntVarP(&parallelism,
		"parallelism",
		"p",
		10,
		"How many queries to send in parallel to http://githubarchive.org",
	)
	backfillCmd.Flags().IntVarP(&retries,
		"retries",
		"r",
		3,
		"How many times to retry failed queries",
	)

	daemonCmd.Flags().DurationVarP(&delay,
		"delay",
		"d",
		0,
		"How long to shift polling to github archive back in time.",
	)
	daemonCmd.Flags().DurationVarP(&period,
		"period",
		"p",
		30*Minute,
		"Polling period",
	)
	daemonCmd.Flags().IntVarP(&retries,
		"retries",
		"r",
		3,
		"How many times to retry per hourly file",
	)
}

func main() {
	// Configs
	if err := RootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(-1)
	}
}
