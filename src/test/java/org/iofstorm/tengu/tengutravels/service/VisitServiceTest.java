package org.iofstorm.tengu.tengutravels.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Mark;
import org.iofstorm.tengu.tengutravels.model.ShortVisit;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisitServiceTest {

    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .disableInnerClassSerialization()
            .registerTypeAdapter(Location.class, new Location.LocationAdapter())
            .registerTypeAdapter(User.class, new User.UserAdapter())
            .registerTypeAdapter(Visit.class, new Visit.VisitAdapter())
            .registerTypeAdapter(ShortVisit.class, new ShortVisit.ShortVisitAdapter())
            .registerTypeAdapter(Mark.class, new Mark.MarkAdapter())
            .create();

    @Test
    public void testUpdateVisit() throws Exception {
        VisitService visitService = new VisitService();
        UserService userServiceMock = mock(UserService.class);
        LocationService locationServiceMock = mock(LocationService.class);

        visitService.setUserService(userServiceMock);
        visitService.setLocationService(locationServiceMock);
        visitService.setGson(gson);

        User andrei = user(1, "Andrei", "m", 22);
        Location spb = location(1, "spb", "ru");
        Visit v1 = visit(1, 1, 1, 123L, 5);

        when(userServiceMock.getUserWithoutLock(1)).thenReturn(andrei);
        when(locationServiceMock.getLocationWithoutLock(1)).thenReturn(spb);

        visitService.createVisit(v1);

        System.out.println();
    }

    private static User user(Integer id, String name, String gender, Integer age) {
        User u = new User();
        u.setId(id);
        u.setFirstName(name);
        u.setGender(gender);
        u.setAge(age);
        return u;
    }

    private static Location location(Integer id, String place, String country) {
        Location l = new Location();
        l.setId(id);
        l.setPlace(place);
        l.setCountry(country);
        return l;
    }

    private static Visit visit(Integer id, Integer userId, Integer locId, Long visitedAt, Integer mark) {
        Visit v = new Visit();
        v.setId(id);
        v.setUserId(userId);
        v.setLocationId(locId);
        v.setVisitedAt(visitedAt);
        v.setMark(mark);
        return v;
    }
}
