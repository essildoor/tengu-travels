package org.iofstorm.tengu.tengutravels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.ZoneId;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    public static final String ID = "id";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String GENDER = "gender";
    public static final String BIRTH_DATE = "birth_date";
    public static final String EMAIL = "email";

    public static final Integer EMAIL_LENGTH = 100;
    public static final Integer NAME_LENGTH = 50;
    public static final Long BIRTH_DATE_MIN = LocalDate.of(1930, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    public static final Long BIRTH_DATE_MAX = LocalDate.of(1999, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();

    // 32 bit int unique
    private Integer id;

    // 0-100 unicode string unique
    private String email;

    // 0-50 unicode string
    @JsonProperty("first_name")
    private String firstName;

    // 0-50 unicode string
    @JsonProperty("last_name")
    private String lastName;

    // m - male, f - female
    private String gender;

    // long ms from 1970
    @JsonProperty("birth_date")
    private Long birthDate;

    @JsonIgnore
    private int age;

    @JsonIgnore
    private ResponseEntity<String> cachedResponse;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Long birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public ResponseEntity<String> getCachedResponse() {
        return cachedResponse;
    }

    public void setCachedResponse(ResponseEntity<String> cachedResponse) {
        this.cachedResponse = cachedResponse;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("email", email)
                .append("firstName", firstName)
                .append("lastName", lastName)
                .append("gender", gender)
                .append("birthDate", birthDate)
                .build();
    }
}
