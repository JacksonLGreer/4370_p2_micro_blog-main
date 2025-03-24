/**
* Create Post endpoint
* Users can upload a post at the top of the home page and it will enter the DB
* Used on home page: http://localhost:8080/
*/
INSERT INTO post (userId, postDate, postText) VALUES (?, ?, ?)

/**
* Get ID of last inserted record
* This is used to attach the postId to a hashtag to act as a foreign key
* Used on home page: http://localhost:8080/
*/
SELECT LAST_INSERT_ID()

/**
* Create hashtag endpoint
* This is used to put hashtags from a post into their own table
* Used on home page: http://localhost:8080/
*/
INSERT INTO hashtag (hashTag, postId) VALUES (?, ?)