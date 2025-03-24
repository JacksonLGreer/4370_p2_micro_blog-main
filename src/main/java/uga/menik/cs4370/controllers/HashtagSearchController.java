/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;
import uga.menik.cs4370.services.UserService;
import uga.menik.cs4370.utility.Utility;

/**
 * Handles /hashtagsearch URL and possibly others.
 * At this point no other URLs.
 */
@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {


    private final DataSource dataSource;
    private final UserService userService;
    public HashtagSearchController(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    /**
     * This function handles the /hashtagsearch URL itself.
     * This URL can process a request parameter with name hashtags.
     * In the browser the URL will look something like below:
     * http://localhost:8081/hashtagsearch?hashtags=%23amazing+%23fireworks
     * Note: the value of the hashtags is URL encoded.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "hashtags") String hashtags) {
        System.out.println("User is searching: " + hashtags);

        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.

        // Implemented by Jackson
        // Will use a REGEX to split the string of hashtags into individual tags in an array
        String regex ="[\\s]";
        String[] hashtagArray = hashtags.split(regex);
        for(int j = 0; j <hashtagArray.length; j++) {
            hashtagArray[j] = hashtagArray[j].substring(1);
            System.out.println(hashtagArray[j]);
        }
        // Creating a dynamic SQL query to get posts that contain all hashtags that were searched
        //
        String placeholders = String.join(", ", Collections.nCopies(hashtagArray.length, "?"));
        String hashtagQuery = """
                SELECT p.postId, p.userId, p.postDate, p.postText, u.username, u.firstName, u.lastName,
                    (SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount,
                    (SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount,
                    EXISTS (SELECT 1 FROM heart WHERE heart.postId = p.postId AND heart.userId = ?) AS isHearted,
                    EXISTS (SELECT 1 FROM bookmark WHERE bookmark.postId = p.postId AND bookmark.userId = ?) AS isBookmarked
                FROM post p
                JOIN `user` u ON p.userId = u.userId
                JOIN hashtag h ON p.postId = h.postId
                WHERE h.hashTag IN (""" + placeholders +") GROUP BY p.postId HAVING COUNT(DISTINCT h.hashtag) = ? ORDER BY p.postDate DESC";
        
        System.out.println(hashtagQuery);
       

        // List for holding the posts that meet the criteria
        List<Post> posts = new ArrayList<Post>();

        String userId = userService.getLoggedInUser().getUserId();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(hashtagQuery)) {
                
                int index = 1;
                pstmt.setString(index++, userId);
                pstmt.setString(index++, userId);
                for (String hashtag : hashtagArray) {
                    pstmt.setString(index++, hashtag);
                }
                pstmt.setInt(index, hashtagArray.length);

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    // This part needs to be fixed
                    User user = new User(
                        rs.getString("userId"), 
                        rs.getString("username"), 
                        rs.getString("firstName"), 
                        rs.getString("lastName")
                    );

                    Post post = new Post( 
                        rs.getString("postId"),
                        rs.getString("postText"),
                        rs.getString("postDate"),
                        user,
                        rs.getInt("heartsCount"),
                        rs.getInt("commentsCount"),
                        rs.getBoolean("isHearted"),
                        rs.getBoolean("isBookmarked")
                    );
                    posts.add(post);
                }
        } catch (SQLException e) {
            e.printStackTrace();
        } // try-catch
        mv.addObject("posts", posts);

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // String errorMessage = "Some error occured!";
        // mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);
        
        return mv;
    }
    
}
