/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;

/**
 * This is a service class that enables user related functions.
 * The class interacts with the database through a dataSource instance.
 * See authenticate and registerUser functions for examples.
 * This service object is spcial. It's lifetime is limited to a user session.
 * Usual services generally have application lifetime.
 */
@Service
@SessionScope
public class UserService {

    // dataSource enables talking to the database.
    private final DataSource dataSource;
    // passwordEncoder is used for password security.
    private final BCryptPasswordEncoder passwordEncoder;
    // This holds 
    private User loggedInUser = null;

    /**
     * See AuthInterceptor notes regarding dependency injection and
     * inversion of control.
     */
    @Autowired
    public UserService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticate user given the username and the password and
     * stores user object for the logged in user in session scope.
     * Returns true if authentication is succesful. False otherwise.
     */
    public boolean authenticate(String username, String password) throws SQLException {
        // Note the ? mark in the query. It is a place holder that we will later replace.
        final String sql = "select * from user where username = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Following line replaces the first place holder with username.
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                // Traverse the result rows one at a time.
                // Note: This specific while loop will only run at most once 
                // since username is unique.
                while (rs.next()) {
                    // Note: rs.get.. functions access attributes of the current row.
                    String storedPasswordHash = rs.getString("password");
                    boolean isPassMatch = passwordEncoder.matches(password, storedPasswordHash);
                    // Note: 
                    if (isPassMatch) {
                        String userId = rs.getString("userId");
                        String firstName = rs.getString("firstName");
                        String lastName = rs.getString("lastName");

                        // Initialize and retain the logged in user.
                        loggedInUser = new User(userId, firstName, lastName);
                    }
                    return isPassMatch;
                }
            }
        }
        return false;
    }

    /**
     * Logs out the user.
     */
    public void unAuthenticate() {
        loggedInUser = null;
    }

    /**
     * Checks if a user is currently authenticated.
     */
    public boolean isAuthenticated() {
        return loggedInUser != null;
    }

    /**
     * Retrieves the currently logged-in user.
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Registers a new user with the given details.
     * Returns true if registration is successful. If the username already exists,
     * a SQLException is thrown due to the unique constraint violation, which should
     * be handled by the caller.
     */
    public boolean registerUser(String username, String password, String firstName, String lastName)
            throws SQLException {
        // Note the ? marks in the SQL statement. They are placeholders like mentioned above.
        final String registerSql = "insert into user (username, password, firstName, lastName) values (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement registerStmt = conn.prepareStatement(registerSql)) {
            // Following lines replace the placeholders 1-4 with values.
            registerStmt.setString(1, username);
            registerStmt.setString(2, passwordEncoder.encode(password));
            registerStmt.setString(3, firstName);
            registerStmt.setString(4, lastName);

            // Execute the statement and check if rows are affected.
            int rowsAffected = registerStmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Gets all of the posts that a user has bookamrked
     * @param userId
     * @return
     */
    public List<Post> getBookmarkedPosts(String userId) {
        List<Post> bookmarkedPosts = new ArrayList<Post>();

        String query = """
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
        """;

        try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            pstmt.setString(3, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
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
                    bookmarkedPosts.add(post);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

            
        return bookmarkedPosts;
    }
    
    public List<Post> getPostsByUserId(String userId) {
        List<Post> posts = new ArrayList<>();
    
        String query = """
            SELECT p.postId, p.userId, p.postDate, p.postText, 
                   u.username, u.firstName, u.lastName,
                   (SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount,
                   (SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount,
                   EXISTS (SELECT 1 FROM heart h WHERE h.postId = p.postId AND h.userId = ?) AS isHearted,
                   EXISTS (SELECT 1 FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) AS isBookmarked
            FROM post p
            JOIN user u ON p.userId = u.userId
            WHERE p.userId = ?
            ORDER BY p.postDate DESC
        """;
    
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
    
            // Set the current user's ID for isHearted and isBookmarked flags
            String currentUserId = getLoggedInUser() != null ? getLoggedInUser().getUserId() : "0";
            pstmt.setString(1, currentUserId);
            pstmt.setString(2, currentUserId);
            pstmt.setString(3, userId);
    
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User postUser = new User(
                        rs.getString("userId"),
                        rs.getString("username"),
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return posts;
    }
    

}
