/**
* Create Post endpoint
* Users can upload a post at the top of the home page and it will enter the DB
* Used on home page: http://localhost:8080/
*/
INSERT INTO post (userId, postDate, postText) VALUES (?, ?, ?)