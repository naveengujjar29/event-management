This project is spring boot powered for event management.

Currently user sign up and sign in funnctionality has been added using the below spring components.
1. Spring Rest
2. Spring Data JPA
3. Spring Security
4. Model Mapper


/api/v1/events
POST API Payload
{    "name": "A.R. Music Show",
    "venue": "Singapore",
    "eventDateTime": "2024-03-01T13:48:18",
    "ticketPrice": 200.0,
    "availableTickets": 120,
    "eventType": "MUSIC_CONCERT",
    "eventState": "ACTIVE"
}
 GET ALL - /api/v1/events
GET Single Event - /api/v1/events/<event Id>
PUT - Update event
DELETE -  /api/v1/events/<event Id>


   
