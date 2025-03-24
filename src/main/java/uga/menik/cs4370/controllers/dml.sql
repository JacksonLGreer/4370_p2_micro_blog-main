/*
* HOME ENDPOINTS
*/

/**
* insertQuery - Create Post endpoint
* Users can upload a post at the top of the home page and it will enter the DB
* Used on home page: http://localhost:8080/
*/
INSERT INTO post (userId, postDate, postText) VALUES (?, ?, ?)

/**
* fetchPostIdQuery - Get ID of last inserted record
* This is used to attach the postId to a hashtag to act as a foreign key
* Used on home page: http://localhost:8080/
*/
SELECT LAST_INSERT_ID()

/**
* getFollowedUsersPosts - Gets a list of all the posts made by users followed by current user
* These are placed on the homepage for viewing
* Used on home page: http://localhost:8080/
*/
SELECT p.postId, p.postText, p.postDate, u.userId, u.username, u.firstName, u.lastName,
    (SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount,
    (SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount,
    EXISTS (SELECT 1 FROM heart h WHERE h.postId = p.postId AND h.userId = ?) AS isHearted,
    EXISTS (SELECT 1 FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) AS isBookmarked
FROM post p
JOIN follow f ON p.userId = f.followeeUserId
JOIN user u ON p.userId = u.userId
WHERE f.followerUserId = ?
ORDER BY p.postDate DESC

/**
* HASHTAG ENDPOINTS
*/

/**
* Create hashtag endpoint
* This is used to put hashtags from a post into their own table
* Used on home page: http://localhost:8080/
*/
INSERT INTO hashtag (hashTag, postId) VALUES (?, ?)

/**
* Dynamic SQL query to search the DB for the user inputted hashtag(s)
* Must be dynamic as there could be any number of hashtags
* Placeholders is generated as String placeholders = String.join(", ", Collections.nCopies(hashtagArray.length, "?"));
* Used on: http://localhost:8080/hashtagsearch?hashtags=%23[hashTag]
*/

SELECT p.postId, p.userId, p.postDate, p.postText, u.username, u.firstName, u.lastName,
    (SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount,
    (SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount,
    EXISTS (SELECT 1 FROM heart WHERE heart.postId = p.postId AND heart.userId = ?) AS isHearted,
    EXISTS (SELECT 1 FROM bookmark WHERE bookmark.postId = p.postId AND bookmark.userId = ?) AS isBookmarked
FROM post p
JOIN `user` u ON p.userId = u.userId
JOIN hashtag h ON p.postId = h.postId
WHERE h.hashTag IN (""" + placeholders +") GROUP BY p.postId HAVING COUNT(DISTINCT h.hashtag) = ? ORDER BY p.postDate DESC

/**
* FOLLOW ENDPOINTS
*/

/**
* followQuery - Used to follow another user
* Activates once the follow button is clicked
* Used on people page: http://localhost:8080/people
*/
INSERT INTO follow (followerUserId, followeeUserId) VALUES (?, ?)

/**
* unfollowQuery - Used to unfollow another user
* Activates once the follow button is clicked
* Used on people page: http://localhost:8080/people
*/
DELETE FROM follow WHERE followerUserId = ? AND followeeUserId = ?

/**
* POST ENDPOINTS
*/

/**
* expandedPostQuery - Used in getting data for displaying an expanded post
* Activates as a user selects a post
* Used on http://localhost:8080/post/[postId]
*/
SELECT p.postId, p.userId, p.postDate, p.postText, u.username, u.firstName, u.lastName,
    (SELECT COUNT(*) FROM heart WHERE heart.postId = p.postId) AS heartsCount,
    (SELECT COUNT(*) FROM comment WHERE comment.postId = p.postId) AS commentsCount,
    EXISTS (SELECT 1 FROM heart WHERE heart.postId = p.postId AND heart.userId = ?) AS isHearted,
    EXISTS (SELECT 1 FROM bookmark WHERE bookmark.postId = p.postId AND bookmark.userId = ?) AS isBookmarked
FROM post p
JOIN user u ON p.userId = u.userId
WHERE p.postId = ?

/**
* commentQuery - Used in getting comments for displaying an expanded post
* Activates as a user selects a post
* Used on http://localhost:8080/post/[postId]
*/
SELECT c.commentId, c.commentText, c.commentDate, u.userId, u.userName, u.firstName, u.lastName
FROM comment c
JOIN user u ON c.userId = u.userId
WHERE c.postId = ?
ORDER BY c.commentDate ASC

/**
* heartQuery - Used to like a post
* Activates whenever a user clicks the heart on an unliked post
* Used on http://localhost:8080/people
*/
INSERT IGNORE INTO heart (postId, userId) VALUES (?, ?)

/**
* unlikeQuery - Used to unlike a post
* Activates whenever a user clicks the heart on an liked post
* Used on http://localhost:8080/people
*/
DELETE FROM heart WHERE postId = ? AND userId = ?

/**
* addBookmarkQuery - Used to bookmark a post
* Activates when a user bookmarks a post
* Used on http://localhost:8080/people
*/
INSERT IGNORE INTO bookmark (postId, userId) VALUES (?, ?)

/**
* removeBookmarkQuery - Used to unbookmark a post
* Activates when a user unbookmarks a post
* Used on http://localhost:8080/people
*/
DELETE FROM bookmark WHERE postId = ? and userid = ?

/**
* getBookmarkedPosts - Used tto retreive bookmarked posts from DB
* Used to display the bookamrks on the bookmark page
* Used on http://localhost:8080/bookmarks
*/
SELECT p.postId, p.userId, p.postDate, p.postText, u.username, u.firstName, u.lastName,
    (SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount,
    (SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount,
    EXISTS (SELECT 1 FROM heart h WHERE h.postId = p.postId AND h.userId = ?) AS isHearted,
    EXISTS (SELECT 1 FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) AS isBookmarked
FROM bookmark b
JOIN post p ON b.postId = p.postId
JOIN user u ON p.userId = u.userId
WHERE b.userId = ?
ORDER BY p.postDate DESC

/**
* PEOPLE ENDPOINTS
*/

/**
* getFollowableUsers - Finds users that are not the current user
* Used to get followable users
* Used on http://localhost:8080/
*/
SELECT u.userId, u.username, u.firstName, u.lastName,
    EXISTS (SELECT 1 FROM follow f WHERE f.followerUserId = ? AND f.followeeUserId = u.userId) AS isFollowed,
    COALESCE((SELECT MAX(p.postDate) FROM post p WHERE p.userId = u.userId), 'Never') AS lastActiveDate
FROM `user` u
WHERE userId != ?
ORDER BY lastActiveDate DESC, firstName, lastName