package main

import (
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/gin-gonic/gin"
)

type album struct {
	ID     string  `json:"id"`
	Title  string  `json:"title"`
	Artist string  `json:"artist"`
	Price  float64 `json:"price"`
}

type ErrorMsg struct {
	Msg string `json:"msg"`
}

type Response struct {
	AlbumID   string `json:"albumID"`
	ImageSize string `json:"imageSize"`
}

var myAlbum album = album{
	ID:     "1",
	Title:  "Hello",
	Artist: "Adele",
	Price:  19.99,
}

func main() {

	router := gin.Default()
	router.GET("/albums", getAlbums)
	router.POST("/albums", postAlbums)
	router.GET("/albums/:id", getAlbums)
	router.Run("8080")
}

func getAlbums(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, myAlbum)
}

func postAlbums(c *gin.Context) {
	image, err := ioutil.ReadAll(c.Request.Body)
	if err != nil {
		badresp := ErrorMsg{
			Msg: "An error occurred",
		}
		c.JSON(http.StatusBadRequest, badresp)
		return
	}

	// Calculate the image size
	imageSize := len(image)

	// Prepare the response
	resp := Response{
		AlbumID:   "123", // Replace with actual Album ID
		ImageSize: fmt.Sprintf("%d bytes", imageSize),
	}

	// Send the JSON response
	c.IndentedJSON(http.StatusOK, resp)
}
