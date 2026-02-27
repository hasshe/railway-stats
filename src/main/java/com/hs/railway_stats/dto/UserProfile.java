package com.hs.railway_stats.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public record UserProfile(
    String firstName,
    String lastName,
    String phone,
    String email,
    String address,
    String city,
    String postalCode,
    String ticketNumber
) {
    public static UserProfile fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            return new UserProfile(
                node.path("firstName").asText("") ,
                node.path("lastName").asText("") ,
                node.path("phone").asText("") ,
                node.path("email").asText("") ,
                node.path("address").asText("") ,
                node.path("city").asText("") ,
                node.path("postalCode").asText("") ,
                node.path("ticketNumber").asText("")
            );
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isComplete() {
        return !firstName.isBlank() && !lastName.isBlank() && !phone.isBlank() &&
               !email.isBlank() && !address.isBlank() && !city.isBlank() &&
               !postalCode.isBlank() && !ticketNumber.isBlank();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

