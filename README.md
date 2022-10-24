# 221021-221024-toby-spring

# 1. 토비의 스프링
## 1.1 지난 시간 복습
- 지난 시간에 토비의 스프링 228p `AddStatement`까지 진행했었다.
- 지난 시간까지 진행했던 코드는 다음과 같다.

`[AWSConnectionMaker.java]`
```java
package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class AWSConnectionMaker implements ConnectionMaker{
    @Override
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        Map<String, String> env = System.getenv();

        //jdbc사용, 드라이버 로드
        Class.forName("com.mysql.cj.jdbc.Driver");

        //db접속
        Connection c = DriverManager.getConnection(env.get("DB_HOST"), env.get("DB_USER"), env.get("DB_PASSWORD"));
        return c;
    }
}
```
`[AddStrategy.java]`
```java
package org.example;

import user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddStrategy implements StatementStrategy {

    private User user;

    public AddStrategy(User user) {
        this.user = user;
    }

    @Override
    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("INSERT INTO users(id, name, password) VALUES(?,?,?)");
        ps.setString(1, user.getID());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        return ps;
    }
}
```
`[ConnectionMaker.java]`
```java
package org.example;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionMaker {
    Connection getConnection() throws ClassNotFoundException, SQLException;
}
```
`[DeleteAllStrategy.java]`
```java
package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteAllStrategy implements StatementStrategy{
    @Override
    public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
        return c.prepareStatement("delete from users");
    }
}
```
`[LocalConnectionMaker.java]`
```java
package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class LocalConnectionMaker implements ConnectionMaker{
    @Override
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        Map<String, String> env = System.getenv();

        //jdbc사용, 드라이버 로드
        Class.forName("com.mysql.cj.jdbc.Driver");

        //db접속
        Connection c = DriverManager.getConnection(env.get("DB_HOST"), env.get("DB_USER"), env.get("DB_PASSWORD"));
        return c;
    }
}
```
`[StatementStrategy.java]`
```java
package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface StatementStrategy {
    PreparedStatement makePreparedStatement(Connection c)throws SQLException;
}
```
`[UserDao.java]`
```java
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

        this.connectionMaker = connectionMaker;
    }

    public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws SQLException{
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = connectionMaker.getConnection();
            ps = new DeleteAllStrategy().makePreparedStatement(c);

            ps.executeUpdate();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if(ps != null){
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }

            if(c != null){
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public void add(User user) throws SQLException {
       StatementStrategy addStrategy = new AddStrategy(user);
       jdbcContextWithStatementStrategy(addStrategy);
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
            if(rs != null){
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if(ps != null){
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }

            if(c != null){
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public void deleteAll() throws SQLException {
        StatementStrategy st = new DeleteAllStrategy();
        jdbcContextWithStatementStrategy(st);
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
            if(rs != null){
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if(ps != null){
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }

            if(c != null){
                try {
                    c.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
```
`[UserDaoFactory.java]`
```java
package org.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDaoFactory {
    @Bean
    public UserDao awsUserDao(){
        AWSConnectionMaker awsConnectionMaker = new AWSConnectionMaker();
        UserDao userDao = new UserDao(awsConnectionMaker);
        return userDao;
    }
    @Bean
    public UserDao localUserDao(){
        UserDao userDao = new UserDao(new LocalConnectionMaker());
        return userDao;
    }
}
```
`[User.java]`
```java
package user;

public class User {
    private String id;
    private String name;
    private String password;
    public User(String id, String name, String password){
        this.id = id;
        this.name = name;
        this.password = password;
    }
    public String getName(){
        return name;
    }

    public String getID(){
        return id;
    }

    public String getPassword(){
        return password;
    }
}
```
`[UserDaoTest.java]`
```java
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
```

## 1.2 DataSource 인터페이스 적용(138p)
- 앞에서 만든 `ConnectionMaker`는 단순히 DB커넥션을 생성해주는 기능 하나만을 정의한 인터페이스이다.
- 하지만 자바에는 이미 DB 커넥션을 가져오는 오브젝트의 기능을 추상화 해서 사용할 수 있게 만들어진 `DataSource`라는 인터페이스가 이미 존재한다. 따라서 `ConnectionMaker`같은 인터페이스를 만들지 않아도 된다.
- 아래 코드들은 각각`DataSource()`를 적용한 `UserDaoFactory.java`와 `UserDao.java`이다.

`[UserDaoFactory.java]`
```java
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class UserDaoFactory {
   @Bean
   UserDao awsUserDao() {
       return new UserDao(awsDataSource());
   }

   @Bean
   UserDao localUserDao() {
       return new UserDao(localDataSource());
   }

   @Bean
   DataSource awsDataSource() {
       Map<String, String> env = System.getenv();
       SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
       dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
       dataSource.setUrl(env.get("DB_HOST"));
       dataSource.setUsername(env.get("DB_USER"));
       dataSource.setPassword(env.get("DB_PASSWORD"));
       return dataSource;
   }
   @Bean
   DataSource localDataSource() {
       Map<String, String> env = System.getenv();
       SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
       dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
       dataSource.setUrl("localhost");
       dataSource.setUsername("root");
       dataSource.setPassword("12345678");
       return dataSource;
   }}
```
`[UserDao.java]`
```java
import javax.sql.DataSource;

public class UserDao {
   private DataSource dataSource; // DataSource를 의존하게 변경
   public UserDao(DataSource dataSource) {
       this.dataSource = dataSource; // 생성자도 변경
   }

…

public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws SQLException {
   Connection c = null;
   PreparedStatement pstmt = null;
   try {
       c = dataSource.getConnection(); // datasource를 사용하게 변경

```
## 1.3 익명 클래스 도입(231p)
- __익명 내부 클래스란?__
![](https://velog.velcdn.com/images/lyj1023/post/8b7ecfbb-9a53-4d6a-9bc2-9e29c682292e/image.png)

- __익명 내부 클래스를 도입한 이유__: `StatementStrategy` Interface의 구현체인 `DeleteAllStrategy()`를 쓰는 곳이 `deleteAll()`한군데 뿐이기 때문에 굳이 class를 새로 만들 필요가 없다. 메소드도 한개 뿐이라서 더욱 그렇다.
- `AddStatement`도 마찬가지로 익명 내부 클래스로 만들 수 있다.
- 아래 코드를 보면 `jdbcContextWithStatementStrategy(new StatementStrategy() {}`를 이용해 익명클래스를 구현했다.

`[UserDao.java]`
```java
public void deleteAll() throws SQLException {
   // "delete from users"
   jdbcContextWithStatementStrategy(new StatementStrategy() {
       @Override
       public PreparedStatement makeStatement(Connection conn) throws SQLException {
           return conn.prepa reStatement("delete from users");
       }
   });
}

public void add(final User user) throws SQLException {
   // DB접속 (ex sql workbeanch실행)
   jdbcContextWithStatementStrategy(new StatementStrategy() {
       @Override
       public PreparedStatement makeStatement(Connection conn) throws SQLException {
           PreparedStatement pstmt = null;
           pstmt = conn.prepareStatement("INSERT INTO users(id, name, password) VALUES(?,?,?);");
           pstmt.setString(1, user.getId());
           pstmt.setString(2, user.getName());
           pstmt.setString(3, user.getPassword());
           return pstmt;
       }
   });
}
```

## 1.4 JdbcContext 분리(234p)
### 1.4.1 jdbcContextWithStatementStrategy의 분리
- __분리하는 이유__: `jdbcContextWithStatementStrategy`는 어디 하나에 종속되지 않고 다른 Dao에서도 사용이 가능하기 때문에 `UserDao`에서 분리한다.(ex. `UserDao`뿐만아니라 `HospitalDao`등에서도 사용하기 위해)

- `jdbcContextWithStatementStrategy`를 분리한 클래스를 `JdbcContext`라고 하고 기존에 `UserDao`에 있던 `jdbcContextWithStatementStrategy`를 `JdbcContext`클래스의 `workWithStatementStrategy`라는 이름으로 바꿔서 넣어준다.

`[JdbcContext.java]`
```java
public class JdbcContext {

   private DataSource dataSource;

   public JdbcContext(DataSource dataSource) {
       this.dataSource = dataSource;
   }

public void workWithStatementStrategy(StatementStrategy stmt) throws SQLException {
   Connection c = null;
   PreparedStatement pstmt = null;
   try {
       c = dataSource.getConnection();
       pstmt = stmt.makeStatement(c);
       // Query문 실행
       pstmt.executeUpdate();
   } catch (SQLException e) {
       throw e;
   } finally {
       if (pstmt != null) {
           try {
               pstmt.close();
           } catch (SQLException e) {
           }
       }
       if (c != null) {
           try {
               c.close();
           } catch (SQLException e) {
           }
       }
     }
   }
}
```
### 1.4.2 UserDao가 JdbcContext의존하게 변경
- 위에서 `JdbcContext` 클래스를 생성했으므로 기존에 `jdbcContextWithStatementStrategy`를 사용하던 `UserDao`도 `JdbcContext`를 의존하도록 바꿔준다.

>- 책에서는 set을 썼는데 이 방식은 xml설정 방식에서 set을 쓰기 때문에 set을 사용하게 써놓았다. 지금은 xml설정 방식을 잘 쓰지 않는다.

![](https://velog.velcdn.com/images/lyj1023/post/3de3ff2a-09cf-43d4-9a5e-488316badd85/image.png)


`[UserDao.java]`
```java
public class UserDao {

   private final DataSource dataSource;
   private final JdbcContext jdbcContext;

   public UserDao(DataSource dataSource) {
       this.dataSource = dataSource;
       this.jdbcContext = new JdbcContext(dataSource);
   }

   public void add(final User user) throws SQLException {
       // DB접속 (ex sql workbeanch실행)
       jdbcContext.workWithStatementStrategy(new StatementStrategy() {
           @Override
           public PreparedStatement makeStatement(Connection conn) throws SQLException {
               PreparedStatement pstmt = null;
               pstmt = conn.prepareStatement("INSERT INTO users(id, name, password) VALUES(?,?,?);");
               pstmt.setString(1, user.getId());
               pstmt.setString(2, user.getName());
               pstmt.setString(3, user.getPassword());
               return pstmt;
           }
       });
   }
```

>- JdbcContext와 DataSource의 관계 설정을 Constructor에서 한 이유
>  ![](https://velog.velcdn.com/images/lyj1023/post/59c87f92-f3da-43b2-8b5c-c5bd40516fdc/image.png)

## 1.5 TemplateCallback적용(247p)
- __TemplateCallback을 적용하는 이유__: 앞에서 사용했던 익명 클래스는 중복되는 부분이 있고 `UserDao`외에도 사용할 가능성이 있다. 따라서 이렇게 재사용 가능한 콜백을 담고 있는 메소드는 Dao가 공유할 수 있는 템플릿 클래스 안으로 옮겨도 된다.
- 익명 클래스를 `JdbcContext` 클래스에 콜백 생성과 템플릿 호출이 담긴 `executeSql()`메소드를 생성해서 옮긴다.
![](https://velog.velcdn.com/images/lyj1023/post/8088f6a9-15f7-404d-adcc-7782a720e41d/image.png)


`[JdbcContext.java]`
```java
public void executeSql(String sql) throws SQLException {
    this.workWithStatementStrategy(new StatementStrategy() {
        @Override
        public PreparedStatement makePreparedStatement(Connection connection) throws SQLException {
            return connection.prepareStatement(sql);
        }
    });
}
```

`[UserDao.java]`
```java
public void deleteAll() throws SQLException {
    this.jdbcContext.executeSql("delete from users");
}
```

## 1.6 스프링의 JdbcTemplate적용(261p)
- 스프링은 JDBC를 이용하는 Dao에서 사용할 수 있도록 준비된 다양한 템플릿과 콜백을 제공한다.
- 따라서 앞에서 만들었던 `JdbcContext`대신 스프링이 제공하는 `JdbcTemplate`을 사용한다.

> - `getCount()` 메소드를 보면 책에선 `queryForInt`을 사용하지만 오래전에 사라졌으므로 `queryForObject`를 사용한다.
> - `queryForObject`는 반환형으로 데이터만 가능하다.
> - 간단하게 쿼리와, 리턴받을 Class를 넘기면 된다. `count(*)`쿼리이기 때문에 Integer.class를 넘기면 된다.

`[UserDao.java]`
```java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class UserDao {

    private final DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource; // 생성자도 변경
        this.jdbcTemplate = new JdbcTemplate(dataSource);

    }

    public void add(final User user) throws SQLException {
        //jdbcTemplate.update()의 경우 두번째부터 파라메터 개수 만큼 ?자리에 값을 넘길 수 있다.
        this.jdbcTemplate.update("insert into users(id, name, password) values (?, ?, ?);",
                user.getID(), user.getName(), user.getPassword());
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        String sql = "select * from users where id = ?";
        RowMapper<User> rowMapper = new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                User user = new User(rs.getString("id"), rs.getString("name"),
                        rs.getString("password"));
                return user;
            }
        };
        return this.jdbcTemplate.queryForObject(sql, rowMapper, id);

    }

    public void deleteAll() throws SQLException {
        this.jdbcTemplate.update("delete from users");
    }

    public int getCount() throws SQLException, ClassNotFoundException {
        //queryForObject에 두번째 파라메터로 Integer.class를 넘겨줌으로써 int형의 데이터를 받아온다.
        return this.jdbcTemplate.queryForObject("select count(*) from users;", Integer.class);
    }
}
```
- 여기서 `get()`메소드는 `RowMapper`를 이용한다.

> *__`RowMapper`란?__
> - 위에서 `queryForObject`는 반환형으로 데이터형만 가능하다고 했는데 만약 `SELECT * FROM USER` 구문으로 User 객체 자체를 반환받고 싶다면? -> `RowMapper`를 이용한다.
> - `RowMapper`를 사용하면, 원하는 형태의 결과값을 반환할 수 있다.
> - `RowMapper`의 `mapRow` 메소드는 `ResultSet`을 사용한다. 사용법은 다음과 같다.
> ` ??? mapRow(ResultSet rs, int count);`
> - `ResultSet`에 값을 담아와서 User 객체에 저장하고 그것을 `count`만큼 반복한다는 뜻이다.

## 1.7 getAll()과 테스트 구현
- `jdbcTemplate.query()` 를 사용한다. 그리고 `List<User> getAll() List<User>` 를 리턴하게 해놓으면 모든 User를 List에 담아서 리턴 해준다.

`[UserDao.java]`
```java
public List<User> getAll() {
    String sql = "select * from users order by id";
    RowMapper<User> rowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User(rs.getString("id"), rs.getString("name"),
                    rs.getString("password"));
            return user;
        }
    };
    return this.jdbcTemplate.query(sql, rowMapper);
}
```
- `getAll()`을 테스트하는 메소드도 테스트 클래스에 만들어 준다.

`[UserDaoTest.java]`
```java
void getAll() throws SQLException {
    userDao.deleteAll();
    List<User> users = userDao.getAll();
    assertEquals(0,users.size());

    userDao.add(user1);
    userDao.add(user2);
    userDao.add(user3);
    users = userDao.getAll();
    assertEquals(3,users.size());
}
```

## 1.8 rowMapper의 중복 제거
- `get()`메소드와 `getAll()`메소드에 `rowMapper`가 중복되므로 따로 메소드로 빼서 중복을 제거해 준다.
- 지금까지 작성했던 `UserDao`의 전체 코드는 다음과 같다.

`[UserDao.java]`
```java
package org.example;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import user.User;
import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;

public class UserDao {

    private final DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource; // 생성자도 변경
        this.jdbcTemplate = new JdbcTemplate(dataSource);

    }

    RowMapper<User> rowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User(rs.getString("id"), rs.getString("name"),
                    rs.getString("password"));
            return user;
        }
    };


    public void add(final User user) throws SQLException {
        this.jdbcTemplate.update("insert into users(id, name, password) values (?, ?, ?);",
                user.getID(), user.getName(), user.getPassword());
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        String sql = "select * from users where id = ?";
        return this.jdbcTemplate.queryForObject(sql, rowMapper, id);
    }

    public void deleteAll() throws SQLException {
        this.jdbcTemplate.update("delete from users");
    }

    public int getCount() throws SQLException, ClassNotFoundException {
        return this.jdbcTemplate.queryForObject("select count(*) from users;", Integer.class);
    }

    public List<User> getAll() {
        String sql = "select * from users order by id";
        return this.jdbcTemplate.query(sql, rowMapper);
    }
}
```

## 1.9 생각해볼 사항들
### 1.9.1 docker가 자꾸 내려간다면?
- AWS가 켜져있는데도 docker가 자꾸 내려간다면 비밀번호 유출에 의한 해킹 가능성이 있다.
- __특정 컨테이너가 종료된 경우 로그 보는 방법__
  ```
  docker container ls -a | grep mysql
  ```
- 아래와 같은 로그가 떴는데 SHUTDOWN을 한적이 없다면 해킹 가능성이 있다.
  ```
  2022-10-21T02:22:01.006922Z 10 [System] [MY-013172] [Server] Received SHUTDOWN from user root. Shutting down mysqld (Version: 8.0.30).
  ```
### 1.9.2 DI할 때 final을 쓰는 이유?
- final이란 한번 초기화 되면 바꿀 수 없는 __불변__하는 성질을 가진다.

- __final을 사용했을때 이점__
  1) 신뢰성 - 불변이기 때문에 변화를 고려하지 않아도 된다.
  2) Memory를 적게 쓴다. - 바뀔 여지가 없기 때문에 바뀌는데 필요한 메모리 할당이 필요 없다.
  3) DI하고 나서 DataSource가 바뀌는 경우 - 무슨일이 일어날지 예측이 안된다.
  
- __final을 사용하는 이유__
  1) Spring에서 DI되었다면 이미 Factory에서 조립이 끝난 상태이므로 변화하지 않는게 좋다.
  2) 변화하지 않는게 좋으므로 final로 쓰는게 좋다. 왜냐하면 메모리 사용에 유리하고 신뢰성 있기 때문이다.
  3) 이후 SpringBoot에서 @Autowired하는 부분이 final로 대체하는 것을 권장하게 바뀌었다.

### 1.9.3 Bean은 언제 사용할까?
- Spring의 ApplicationContext에 등록하는 빈을 만들 때 `ex) AwsUserDao` 등 재료가 되는 @Bean 도 붙여주는 경우가 있다. 조립의 재료로만 쓴다면 ApplicationContext에 등록을 안해줘도 된다.


<br/>

# 2. Reference
- __우선순위 큐__ 출처: https://velog.io/@gillog/Java-Priority-Queue%EC%9A%B0%EC%84%A0-%EC%88%9C%EC%9C%84-%ED%81%90
- __`queryForObject`의 사용__ 출처: https://krksap.tistory.com/1995
- __`RowMapper`란?__ 출처: https://velog.io/@seculoper235/RowMapper%EC%97%90-%EB%8C%80%ED%95%B4
