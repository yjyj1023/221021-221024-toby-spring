package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddStrategy implements StatementStrategy {
    @Override
    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("INSERT INTO users(id, name, password) VALUES(?,?,?)");
//        ps.setString(1, user.getID());
//        ps.setString(2, user.getName());
//        ps.setString(3, user.getPassword());
//
        return null;
    }
}
