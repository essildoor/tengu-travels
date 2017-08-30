package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.controller.ControllerHelper;
import org.iofstorm.tengu.tengutravels.model.User;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class UserServiceTest {

    @Test(dataProvider = "getUserDataProvider")
    public void testGetUser(Integer id, User expectedResult) throws Exception {
        UserService userService = new UserService(null, null);
        User testUser = new User();
        testUser.setId(1);
        userService.addUserForTest(testUser);
        User actual = userService.getUser(id);

        if (expectedResult != null) assertThat(actual, samePropertyValuesAs( expectedResult ));
        else assertNull( actual );
    }

    @Test(dataProvider = "userExistDataProvider")
    public void testUserExist(Integer id, Boolean expectedResult) throws Exception {
        UserService userService = new UserService(null, null);
        User u = new User();
        u.setId(1);
        userService.addUserForTest(u);
        assertThat(userService.userExist(id), is(equalTo(expectedResult)));
    }

    @Test(dataProvider = "createUserDataProvider")
    public void testCreateUser(User user, Integer expectedCode) throws Exception {
        Utils utilsMock = mock(Utils.class);
        UserService userService = new UserService(utilsMock, null);

        User existingUser = new User();
        existingUser.setId(1);
        userService.addUserForTest(existingUser);

        Integer actualCode = userService.createUser(user);

        assertThat(actualCode, is(equalTo(expectedCode)));
        assertTrue(userService.userExist(user.getId()));
    }

    @Test(dataProvider = "updateUserDataProvider")
    public void testUpdateUser(Integer id, User userToUpdate, User existingUser, Integer expectedStatus, User expectedUser) throws Exception {
        UserService userService = new UserService(mock(Utils.class), null);
        if (existingUser != null) userService.addUserForTest(existingUser);

        Integer actualStatus = userService.updateUser(id, userToUpdate);

        User actualUser = userService.getUser(id);

        assertThat(actualStatus, is(equalTo(expectedStatus)));
        if (expectedUser != null) assertThat(actualUser, samePropertyValuesAs(expectedUser));
        else assertNull(actualUser);
    }

    @DataProvider(name = "getUserDataProvider")
    public static Object[][] getUserDataProvider() {
        User u = new User();
        u.setId(1);
        return new Object[][]{
                {1, u},
                {null, null},
                {2, null}
        };
    }

    @DataProvider(name = "userExistDataProvider")
    public static Object[][] userExistDataProvider() {
        return new Object[][]{
                {1, Boolean.TRUE},
                {2, Boolean.FALSE},
                {null, Boolean.FALSE}
        };
    }

    @DataProvider(name = "createUserDataProvider")
    public static Object[][] Name() {
        return new Object[][]{
                {shallowUser(1), ControllerHelper.BAD_REQUEST},
                {shallowUser(2), ControllerHelper.OK}
        };
    }

    @DataProvider(name = "updateUserDataProvider")
    public static Object[][] updateUserDataProvider() {
        User existingUser = shallowUser(1);
        existingUser.setEmail("old@ya.ru");
        existingUser.setFirstName("vasya");
        existingUser.setLastName("pupkin");
        existingUser.setBirthDate(556577660L);
        existingUser.setGender("m");

        User userToUpdate1 = new User();
        userToUpdate1.setEmail("new@ya.ru");
        userToUpdate1.setFirstName("lena");
        userToUpdate1.setLastName("golovach");
        userToUpdate1.setBirthDate(619736060L);
        userToUpdate1.setGender("f");

        User expectedUser1 = shallowUser(1);
        expectedUser1.setEmail("new@ya.ru");
        expectedUser1.setFirstName("lena");
        expectedUser1.setLastName("golovach");
        expectedUser1.setBirthDate(619736060L);
        expectedUser1.setGender("f");

        return new Object[][]{
                {1, userToUpdate1, existingUser, ControllerHelper.OK, expectedUser1}
        };
    }

    private static User shallowUser(Integer id) {
        User u = new User();
        u.setId(id);
        return u;
    }
}
