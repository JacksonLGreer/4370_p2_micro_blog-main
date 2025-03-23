create database if not exists cs4370_mb_platform;

-- Use the created database.
use cs4370_mb_platform;

-- Create the user table.
create table if not exists user (
    userId int auto_increment,
    username varchar(255) not null,
    password varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    primary key (userId),
    unique (username),
    constraint userName_min_length check (char_length(trim(userName)) >= 2),
    constraint firstName_min_length check (char_length(trim(firstName)) >= 2),
    constraint lastName_min_length check (char_length(trim(lastName)) >= 2)
);

-- Create the post table
create table if not exists post (
    postId INT AUTO_INCREMENT,     
    userId INT NOT NULL,
    postDate VARCHAR(255) NOT NULL,             
    postText TEXT NOT NULL,
    PRIMARY KEY (postId),
    FOREIGN KEY (userId) REFERENCES user(userId)
);

-- Create the comment table
create table if not exists comment (
    commentId INT AUTO_INCREMENT,
    postId INT NOT NULL,
    userId INT NOT NULL,
    commentDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    commentText TEXT NOT NULL,
    PRIMARY KEY (commentId),
    FOREIGN KEY (postId) REFERENCES post(postId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES user(userId) ON DELETE CASCADE
);

-- Create the heart table
create table if not exists heart (
    postId INT NOT NULL,
    userId INT NOT NULL,
    PRIMARY KEY (postId, userId),
    FOREIGN KEY (postId) REFERENCES post(postId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES user(userId) ON DELETE CASCADE
);

-- Create the bookmark table
create table if not exists bookmark (
    postId INT NOT NULL,
    userId INT NOT NULL,
    PRIMARY KEY (postId, userId),
    FOREIGN KEY (postId) REFERENCES post(postId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES user(userId) ON DELETE CASCADE
);

-- Create the hashtag table
create table if not exists hashtag (
    hashTag VARCHAR(100) NOT NULL,
    postId INT NOT NULL,
    PRIMARY KEY (hashTag, postId),
    FOREIGN KEY (postId) REFERENCES post(postId) ON DELETE CASCADE
);

-- Create the follow table
create table if not exists follow (
    followerUserId INT NOT NULL,
    followeeUserId INT NOT NULL,
    PRIMARY KEY (followerUserId, followeeUserId),
    FOREIGN KEY (followerUserId) REFERENCES user(userId) ON DELETE CASCADE,
    FOREIGN KEY (followeeUserId) REFERENCES user(userId) ON DELETE CASCADE
);

