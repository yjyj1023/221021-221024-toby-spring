package org.example;

import org.springframework.dao.EmptyResultDataAccessException;
import user.User;

import java.sql.*;
import java.util.Map;

public class UserDao {
    private ConnectionMaker connectionMaker;

    public UserDao() {
        this.connectionMaker = new AWSConnectionMaker();
    }

    public UserDao(ConnectionMaker connectionMaker) {

        this.connectionMaker = new AWSConnectionMaker();
    }

    public void add(User user) throws SQLException, ClassNotFoundException {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            //db접속
            c = connectionMaker.getConnection();

            //쿼리문 작성(insert)
            ps = c.prepareStatement("INSERT INTO users(id, name, password) VALUES(?,?,?)");
            ps.setString(1, user.getID());
            ps.setString(2, user.getName());
            ps.setString(3, user.getPassword());

            //쿼리문 실행
            ps.executeUpdate();

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
            }
            try {
                c.close();
            } catch (SQLException e) {
            }
        }
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            //db접속
            c = connectionMaker.getConnection();

            //쿼리문 작성(select)
            ps = c.prepareStatement("SELECT id,name,password FROM users WHERE id = ?");
            ps.setString(1, id);

            //executeQuery: resultset객체에 결과집합 담기, 주로 select문에서 실행
            rs = ps.executeQuery();

            User user = null;
            //select문의 존재여부 확인(다음 행이 존재하면 true 리턴)
            if(rs.next()){
                user = new User(rs.getString("id"),
                        rs.getString("name"), rs.getString("password"));
            }

            if(user == null) throw new EmptyResultDataAccessException(1);
            return user;

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
            }
            try {
                ps.close();
            } catch (SQLException e) {
            }
            try {
                c.close();
            } catch (SQLException e) {
            }
        }
    }

    public void deleteAll() throws SQLException, ClassNotFoundException {

        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = connectionMaker.getConnection();
            ps = c.prepareStatement("delete from users");

            ps.executeUpdate();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
            }

            try {
                c.close();
            } catch (SQLException e) {
            }
        }
    }

    public int getCount() throws SQLException, ClassNotFoundException {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            c = connectionMaker.getConnection();
            ps = c.prepareStatement("select count(*) from users");

            rs = ps.executeQuery();
            rs.next();

            return rs.getInt(1);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                rs.close();
            } catch (SQLException e) {
            }
            try {
                ps.close();
            } catch (SQLException e) {
            }
            try {
                c.close();
            } catch (SQLException e) {
            }
        }
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        UserDao userDao = new UserDao();
        userDao.add(new User("15", "Ruru", "1534qwer"));
//        User user = userDao.get("1");
//        System.out.println(user.getName());
    }
}