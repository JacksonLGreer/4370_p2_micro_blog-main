/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;


import java.sql.Connection;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.utility.Utility;
import uga.menik.cs4370.services.UserService;
import uga.menik.cs4370.models.User;
/**
 * This controller handles the home page and some of it's sub URLs.
 */
@Controller
@RequestMapping
public class HomeController {

    // setup datasource
    private final DataSource dataSource;
    private final UserService userService;
    public HomeController(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }
    /**
     * This is the specific function that handles the root URL itself.
     * 
     * Note that this accepts a URL parameter called error.
     * The value to this parameter can be shown to the user as an error message.
     * See notes in HashtagSearchController.java regarding URL parameters.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        // get current User
        User user = userService.getLoggedInUser();
        
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("home_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.
        //List<Post> posts = Utility.createSamplePostsListWithoutComments();
        List<Post> posts = getFollowedUsersPosts(user.getUserId());

        mv.addObject("posts", posts);

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
     * This function handles the /createpost URL.
     * This handles a post request that is going to be a form submission.
     * The form for this can be found in the home page. The form has a
     * input field with name = posttext. Note that the @RequestParam
     * annotation has the same name. This makes it possible to access the value
     * from the input from the form after it is submitted.
     */
    @PostMapping("/createpost")
    public String createPost(@RequestParam(name = "posttext") String postText) {
        System.out.println("User is creating post: " + postText);

        // Redirect the user if the post creation is a success.
        // return "redirect:/";

        // Implementation by Jackson
        User user = userService.getLoggedInUser();
        // Queries
        String insertQuery = "INSERT INTO post (userId, postDate, postText) VALUES (?, ?, ?)";
        String fetchPostIdQuery = "SELECT LAST_INSERT_ID()";
        String insertHashtagQuery = "INSERT INTO hashtag (hashTag, postId) VALUES (?, ?)";
        // Connect to database
        try (Connection conn = dataSource.getConnection();
            // Create a prepared statement to send to DB
            PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setString(1, user.getUserId());
                String uploadTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a"));
                pstmt.setString(2, uploadTime);
                pstmt.setString(3, postText);
                int rowsAffected = pstmt.executeUpdate();
                // If upload successful then enter if
                if (rowsAffected > 0) {
                    // Get the post ID from the new post so we can make FK to hashtag
                    int postId = -1;
                    try (PreparedStatement idStmt = conn.prepareStatement(fetchPostIdQuery)) {
                        ResultSet rs = idStmt.executeQuery();
                        if (rs.next()) {
                            postId = rs.getInt(1);
                        }
                    }
                    if (postId > 0) {
                         // Extract the hashtags and then put them into the hashtag table
                        List<String> hashtags = extractHashtags(postText);
                        try (PreparedStatement hashtagStmt = conn.prepareStatement(insertHashtagQuery)) {
                            for (String hashtag : hashtags) {
                                hashtagStmt.setString(1, hashtag);
                                hashtagStmt.setInt(2, postId);
                                hashtagStmt.executeUpdate();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        return "redirect:/";
                    }
                   
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
            };
        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to create the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/?error=" + message;
    }

    /**
     * Function to extract hashtags from the postText
     * @param postText - The text from the post to be parsed through
     * @return Array list containing all the hashtag text
     */
    public List<String> extractHashtags(String postText) {
        Pattern hashtagPat = Pattern.compile("#(\\S+)");
        Matcher mat = hashtagPat.matcher(postText);
        List<String> hashtags = new ArrayList<String>();
        while (mat.find()) {
            //System.out.println(mat.group(1));
            hashtags.add(mat.group(1));
        }
        return hashtags;
    }

    public List<Post> getFollowedUsersPosts(String userId) {
        List<Post> posts = new ArrayList<Post>();

        String query = """
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
                """;

                try (Connection conn = dataSource.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setString(1, userId);
                        pstmt.setString(2, userId);
                        pstmt.setString(3, userId);

                        ResultSet rs = pstmt.executeQuery();

                        while (rs.next()) {
                            User postUser = new User(
                                rs.getString("userId"), 
                                rs.getString("firstName"), 
                                rs.getString("lastName")
                            );

                            Post post = new Post( 
                                rs.getString("postId"),
                                rs.getString("postText"),
                                rs.getString("postDate"),
                                postUser,
                                rs.getInt("heartsCount"),
                                rs.getInt("commentsCount"),
                                rs.getBoolean("isHearted"),
                                rs.getBoolean("isBookmarked")
                            );
                            posts.add(post);

                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

        return posts;
    }

}
