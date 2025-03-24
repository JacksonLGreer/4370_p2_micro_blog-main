/**
 Copyright (c) 2024 Sami Menik, PhD. All rights reserved.
 
 This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
 */
package uga.menik.cs4370.controllers;
 
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.mysql.cj.x.protobuf.MysqlxPrepare.Prepare;

import uga.menik.cs4370.models.Comment;
import uga.menik.cs4370.models.ExpandedPost;
import uga.menik.cs4370.models.User;
import uga.menik.cs4370.services.PeopleService;
import uga.menik.cs4370.services.UserService;
import uga.menik.cs4370.utility.Utility;

/**
 * Handles /post URL and its sub urls.
 */
@Controller
@RequestMapping("/post")
public class PostController {

    private final UserService userService;
    private final PeopleService peopleService;
    private final DataSource dataSource;

    public PostController (UserService userService, PeopleService peopleService, DataSource dataSource) {
        this.userService = userService;
        this.peopleService = peopleService;
        this.dataSource = dataSource;
    }

    /**
     * This function handles the /post/{postId} URL.
     * This handlers serves the web page for a specific post.
     * Note there is a path variable {postId}.
     * An example URL handled by this function looks like below:
     * http://localhost:8081/post/1
     * The above URL assigns 1 to postId.
     * 
     * See notes from HomeController.java regardig error URL parameter.
     */
    @GetMapping("/{postId}")
    public ModelAndView webpage(@PathVariable("postId") String postId,
            @RequestParam(name = "error", required = false) String error) {
        System.out.println("The user is attempting to view post with id: " + postId);
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.

        String postQuery = """
                SELECT p.postId, p.userId, p.postDate, p.postText, u.username, u.firstName, u.lastName,
                    (SELECT COUNT(*) FROM heart WHERE heart.postId = p.postId) AS heartsCount,
                    (SELECT COUNT(*) FROM comment WHERE comment.postId = p.postId) AS commentsCount,
                    EXISTS (SELECT 1 FROM heart WHERE heart.postId = p.postId AND heart.userId = ?) AS isHearted,
                    EXISTS (SELECT 1 FROM bookmark WHERE bookmark.postId = p.postId AND bookmark.userId = ?) AS isBookmarked
                FROM post p
                JOIN user u ON p.userId = u.userId
                WHERE p.postId = ?
            """;

        String commentQuery = """
                SELECT c.commentId, c.commentText, c.commentDate, u.userId, u.userName, u.firstName, u.lastName
                FROM comment c
                JOIN user u ON c.userId = u.userId
                WHERE c.postId = ?
                ORDER BY c.commentDate ASC
            """;

        ExpandedPost expandedPost = null;

        try (Connection conn = dataSource.getConnection();
        PreparedStatement postStmt = conn.prepareStatement(postQuery);
        PreparedStatement commentStmt = conn.prepareStatement(commentQuery)) {

            String userId = userService.getLoggedInUser().getUserId();

            // setting params for post
            postStmt.setString(1, userId);
            postStmt.setString(2, userId);
            postStmt.setString(3, postId);

            ResultSet postRs = postStmt.executeQuery();

            if (postRs.next()) {
                User user = new User(
                    postRs.getString("userId"),
                    postRs.getString("username"),
                    postRs.getString("firstName"),
                    postRs.getString("lastName")
                );

                // fetch comments
                commentStmt.setString(1, postId);
                ResultSet commentRs = commentStmt.executeQuery();
                List<Comment> comments = new ArrayList<Comment>();

                while (commentRs.next()) {
                    User commentUser = new User(
                        commentRs.getString("userId"),
                        commentRs.getString("username"),
                        commentRs.getString("firstName"),
                        commentRs.getString("lastName")
                    );

                    Comment comment = new Comment (
                        commentRs.getString("commentId"),
                        commentRs.getString("commentText"),
                        commentRs.getString("commentDate"),
                        commentUser
                    );
                    comments.add(comment);
                }

                // Construct the whole post
                expandedPost = new ExpandedPost(
                    postRs.getString("postId"),
                    postRs.getString("postText"),
                    postRs.getString("postDate"),
                    user,
                    postRs.getInt("heartsCount"),
                    postRs.getInt("commentsCount"),
                    postRs.getBoolean("isHearted"),
                    postRs.getBoolean("isBookmarked"),
                    comments
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (expandedPost != null) {
            mv.addObject("posts", List.of(expandedPost));
        } else {
            mv.addObject("isNoContent", true);
        }
        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // An error message can be optionally specified with a url query parameter too.
        String errorMessage = error;
        mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);

        return mv;
    }

    /**
     * Handles comments added on posts.
     * See comments on webpage function to see how path variables work here.
     * This function handles form posts.
     * See comments in HomeController.java regarding form submissions.
     */
    @PostMapping("/{postId}/comment")
    public String postComment(@PathVariable("postId") String postId,
            @RequestParam(name = "comment") String comment) {
        System.out.println("The user is attempting add a comment:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tcomment: " + comment);

        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;

        // Implementation by Jackson
        String userId = userService.getLoggedInUser().getUserId();
        String commentQuery = "INSERT INTO comment (postId, userId, commentDate, commentText) VALUES (?, ?, NOW(), ?)";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(commentQuery)) {

            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            pstmt.setString(3, comment);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                return "redirect:/post/" + postId; // Success: Redirect to the post
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to post the comment. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

    /**
     * Handles likes added on posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions and how path variables work.
     */
    @GetMapping("/{postId}/heart/{isAdd}")
    public String addOrRemoveHeart(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a heart:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;

        //Implementation by Miroslav
        String userId = userService.getLoggedInUser().getUserId();
        String sql;

        try (Connection conn = dataSource.getConnection()) {
            if (isAdd) {
                sql = "INSERT IGNORE INTO heart (postId, userId) VALUES (?, ?)";
            } else {
                sql = "DELETE FROM heart WHERE postId = ? AND userId = ?";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, postId);
                stmt.setString(2, userId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to (un)like the post. Please try again.", StandardCharsets.UTF_8);
            return "redirect:/post/" + postId + "?error=" + message;
        }

        return "redirect:/post/" + postId;
    }

    /**
     * Handles bookmarking posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions.
     */
    @GetMapping("/{postId}/bookmark/{isAdd}")
    public String addOrRemoveBookmark(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a bookmark:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;

        // Implementation by Jackson
        User loggedInUser = userService.getLoggedInUser();
        String userId = loggedInUser.getUserId();

        //connect to DB
        try (Connection conn = dataSource.getConnection()) {
            if (isAdd) {
                //Adding a bookmark
                String addBookmarkQuery = "INSERT IGNORE INTO bookmark (postId, userId) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(addBookmarkQuery)) {
                    pstmt.setString(1, postId);
                    pstmt.setString(2, userId);
                    pstmt.executeUpdate();
                }
            } else {
                // Removing a bookmark
                String removeBookmarkQuery = "DELETE FROM bookmark WHERE postId = ? and userid = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(removeBookmarkQuery)) {
                    pstmt.setString(1, postId);
                    pstmt.setString(2, userId);
                    pstmt.executeUpdate();
                }
            } // ifelse
            return "redirect:/post/" + postId;
        } catch (SQLException e) {
            e.printStackTrace();
        }// trycatch
        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to (un)bookmark the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

}