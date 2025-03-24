/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.services;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import uga.menik.cs4370.models.FollowableUser;
import uga.menik.cs4370.utility.Utility;

/**
 * This service contains people related functions.
 */
@Service
public class PeopleService {
    // setup datasource
    private final DataSource dataSource;
    public PeopleService(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    /**
     * This function should query and return all users that 
     * are followable. The list should not contain the user 
     * with id userIdToExclude.
     */
    public List<FollowableUser> getFollowableUsers(String userIdToExclude) {
        List<FollowableUser> followableUsers = new ArrayList<>();
        // Write an SQL query to find the users that are not the current user.
        String query = "SELECT userId, firstName, lastName FROM user WHERE userId != ?";
        // Run the query with a datasource.
        // See UserService.java to see how to inject DataSource instance and
        // use it to run a query.
        try (Connection conn = dataSource.getConnection();
            PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, userIdToExclude);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String userId = rs.getString("userId");
                        String firstName = rs.getString("firstName");
                        String lastName = rs.getString("lastName");
                        followableUsers.add(new FollowableUser(userId, firstName, lastName, false, "Mar 10, 2025"));
                    }
                } // try
            }
        catch (SQLException e) {
            e.printStackTrace();
        } // try catch
        // Use the query result to create a list of followable users.
        // See UserService.java to see how to access rows and their attributes
        // from the query result.
        // Check the following createSampleFollowableUserList function to see 
        // how to create a list of FollowableUsers.

        // Replace the following line and return the list you created.
        return followableUsers;
    }

    public List<FollowableUser> getAllUsersExceptCurrent(String loggedInUserId) {
        List<FollowableUser> users = new ArrayList<FollowableUser>();

        String query = """
            SELECT u.userId, u.username, u.firstName, u.lastName,
                EXISTS (SELECT 1 FROM follow f WHERE f.followerUserId = ? AND f.followeeUserId = u.userId) AS isFollowed,
                COALESCE((SELECT MAX(p.postDate) FROM post p WHERE p.userId = u.userId), 'Never') AS lastActiveDate
            FROM `user` u
            WHERE userId != ?
            ORDER BY lastActiveDate DESC, firstName, lastName
        """;

        try (Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, loggedInUserId);
                pstmt.setString(2, loggedInUserId);

                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    users.add(new FollowableUser( 
                        rs.getString("userId"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getBoolean("isFollowed"),
                        rs.getString("lastActiveDate")
                    ));
                }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

}
