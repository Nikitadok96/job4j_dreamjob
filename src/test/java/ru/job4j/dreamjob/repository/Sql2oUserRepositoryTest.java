package ru.job4j.dreamjob.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sql2o.Sql2o;
import ru.job4j.dreamjob.configuration.DatasourceConfiguration;
import ru.job4j.dreamjob.model.User;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class Sql2oUserRepositoryTest {
    private static Sql2oUserRepository sql2oUserRepository;
    private static Sql2o sql2o;

    @BeforeAll
    public static void initRepositories() throws Exception {
        var properties = new Properties();
        try (var inputStream = Sql2oUserRepositoryTest.class.getClassLoader().getResourceAsStream("connection.properties")) {
            properties.load(inputStream);
        }
        var url = properties.getProperty("datasource.url");
        var username = properties.getProperty("datasource.username");
        var password = properties.getProperty("datasource.password");

        var configuration = new DatasourceConfiguration();
        var datasource = configuration.connectionPool(url, username, password);
        sql2o = configuration.databaseClient(datasource);

        sql2oUserRepository = new Sql2oUserRepository(sql2o);
    }

    @AfterEach
    private void clearUsers() {
        try (var connection = sql2o.open()) {
            connection.createQuery("DELETE FROM users").executeUpdate();
        }
    }

    @Test
    public void whenSaveUserThenGet() {
        User expectedUser = new User(0, "mail", "name", "pass");
        sql2oUserRepository.save(expectedUser);
        Optional<User> findUser = sql2oUserRepository.findByEmailAndPassword("mail", "pass");
        assertThat(expectedUser).isEqualTo(findUser.get());
    }

    @Test
    public void whenDontSaveThenNothingFound() {
        Optional<User> user = sql2oUserRepository.findByEmailAndPassword("mail", "pass");
        assertThat(user).isEmpty();
    }

    @Test
    public void whenSaveTwoDifferentUser() {
        User user1 = new User(0, "mail1", "name1", "pass");
        User user2 = new User(0, "mail2", "name2", "pass");
        sql2oUserRepository.save(user1);
        sql2oUserRepository.save(user2);
        Optional<User> findFirstUser = sql2oUserRepository.findByEmailAndPassword("mail1", "pass");
        Optional<User> findSecondUser = sql2oUserRepository.findByEmailAndPassword("mail2", "pass");
        assertThat(user1).isEqualTo(findFirstUser.get());
        assertThat(user2).isEqualTo(findSecondUser.get());
    }

    @Test
    public void whenTrySaveTwoUserIdenticalEmail() {
        User user1 = new User(0, "mail1", "name1", "pass");
        User user2 = new User(0, "mail1", "name2", "pass");
        sql2oUserRepository.save(user1);
        sql2oUserRepository.save(user2);
        Optional<User> findUser = sql2oUserRepository.findByEmailAndPassword("mail1", "pass");
        assertThat(user1).isEqualTo(findUser.get());
        assertThat(user2).isNotEqualTo(findUser.get());
        assertThat(user2.getId()).isEqualTo(0);
    }
}