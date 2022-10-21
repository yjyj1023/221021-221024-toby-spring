package org.example;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserDaoFactory.class)
class UserDaoTest {

    @Autowired
    ApplicationContext context;

    UserDao userDao;
    User user1;
    User user2;
    User user3;

    @BeforeEach
    void setUp(){
        this.userDao = context.getBean("awsUserDao", UserDao.class);
        user1 = new User("1","박성철","1234");
        user2 = new User("2","이길원","1321");
        user3 = new User("3","박범진","4321");
        System.out.println("beforeEach 실행");
    }

    @Test
    void addAndSelect() throws SQLException, ClassNotFoundException {

        //컬럼삭제
        userDao.deleteAll();
        assertEquals(0,userDao.getCount());

        userDao.add(user1);
        assertEquals(1,userDao.getCount());
        User user = userDao.get(user1.getID());

        Assertions.assertEquals(user1.getName(),user.getName());
        Assertions.assertEquals(user1.getPassword(),user.getPassword());

    }

    @Test
    void count() throws SQLException, ClassNotFoundException {

        userDao.deleteAll();
        assertEquals(0,userDao.getCount());

        userDao.add(user1);
        assertEquals(1,userDao.getCount());

        userDao.add(user2);
        assertEquals(2,userDao.getCount());

        userDao.add(user3);
        assertEquals(3,userDao.getCount());

    }

    @Test
    void get() {
        assertThrows(EmptyResultDataAccessException.class, ()-> {
            userDao.get("30");
        });
    }
}