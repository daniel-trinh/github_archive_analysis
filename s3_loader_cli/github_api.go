package main

import (
	g "github.com/google/go-github/github"
	"golang.org/x/oauth2"
	"fmt"
	"github.com/spf13/cobra"
	"encoding/json"
	"time"
	log "github.com/Sirupsen/logrus"

	"errors"
	"strconv"
	"compress/gzip"
	"bytes"
)

func repos(cmd *cobra.Command, args []string) {
	ts := oauth2.StaticTokenSource(
		&oauth2.Token{AccessToken: githubToken},
	)
	tc := oauth2.NewClient(oauth2.NoContext, ts)
	client := g.NewClient(tc)

	// list all repositories for the authenticated user

	var stream <-chan g.Repository
	if language == "" {
		stream = listRepositories(client, since, retries, limit)
	} else {
		stream = searchReposByLanguage(client, language, retries, limit)
	}

	for repo := range stream {
		json, err := json.Marshal(repo)
		if err != nil {
			log.Error(err.Error())
		}

		if (gzipEnabled) {
			var b bytes.Buffer
			gz := gzip.NewWriter(&b)
			if _, err := gz.Write([]byte(string(json) + "\n")); err != nil {
				panic(err)
			}
			if err := gz.Flush(); err != nil {
				panic(err)
			}
			if err := gz.Close(); err != nil {
				panic(err)
			}
			fmt.Print(b.String())
		} else {
			fmt.Print(string(json) + "\n")
		}
	}
}

func searchReposByLanguage(c *g.Client, language string, retries, limit int) <-chan g.Repository {
	limitTicker := time.NewTicker(time.Minute / 30)
	q := "language:" + language
	currentPage := 0
	currentLimit := 0
	results := make(chan g.Repository, 1000)

	searchResults, response, err := searchRepo(c, q, currentPage, retries)

	go func() {
		for currentPage <= response.LastPage || currentLimit > limit {
			select {
			case <-limitTicker.C:
				searchResults, response, err = searchRepo(c, q, currentPage, retries)
				if err != nil {
					log.Error(err.Error())
				} else {
					currentPage = response.NextPage
					for _, repo := range searchResults {
						results <- repo
					}
				}
			}
		}
		close(results)
	}()

	return results
}

func searchRepo(c *g.Client, q string, page, retries int) ([]g.Repository, *g.Response, error) {
	for i := 0; i < retries; i++ {
		searchResults, response, err := c.Search.Repositories(
			q,
			&g.SearchOptions{ListOptions: g.ListOptions{page, 100}},
		)
		if err == nil {
			return searchResults.Repositories, response, nil
		}
		sleepIfLimitReached(&response.Rate)
	}
	return nil, nil, errors.New("Retries reached while searching repos with " + q + " on page " + strconv.Itoa(page))
}

func sleepIfLimitReached(r *g.Rate) {
	if r.Remaining == 0 {
		sleepTime := r.Reset.Time.UTC().Sub(time.Now().UTC())
		log.Warn("Rate limit reached, sleeping til " + r.Reset.Time.Format("2016-01-01:01:59:59"))
		time.Sleep(sleepTime)
	}
}

func listRepositories(c *g.Client, since int, retries int, limit int) <-chan g.Repository {
	limitTicker := time.NewTicker(time.Hour / 5000)
	results := make(chan g.Repository, 10000)

	currentSince := since
	currentLimit := -1
	var err error

	getPages := func() {
		for err == nil && currentSince >= 0 && currentLimit < limit {
			select {
			case <-limitTicker.C:
				repos, newSince, err := listRepositoryPage(c, currentSince, retries)

				if err != nil {
					log.Error(err.Error())
					log.Error(errors.New("Retries exhausted. Last since: " + strconv.Itoa(since)))
				} else {
					currentSince = newSince
					for _, repo := range repos {
						results <- repo
					}
				}
				if limit > 0 {
					currentLimit = currentSince - since
				}
			}
		}
	}

	go getPages()

	return results
}

// Should not run on the main thread, can potentially block due to sleep
func listRepositoryPage(c *g.Client, since, retries int) ([]g.Repository, int, error) {
	for i := 0; i < retries; i++ {
		repos, response, err := c.Repositories.ListAll(&g.RepositoryListAllOptions{
			Since: since,
		})

		if err != nil {
			log.Error(err.Error())
		}
		sleepIfLimitReached(&response.Rate)
		var newSince int
		if index := len(repos) - 1; index > 0 {
			newSince = *repos[index].ID
		} else {
			newSince = -1
		}
		return repos, newSince, nil
	}
	return nil, -1, errors.New("Retries exhausted")
}