# Welcome to Event Management Project 

This event management project is spring boot based implementation project which is aiming to cover the event management functionality.

This functionality aims three personas:

 1. Customer
 2. Event Manager
 3. Ticket officer

## Booking Flow Step By Step

We are going to cover step by steps APIs available to book the event.

 1. Sign Up and Sign In of the Customer
	


> First sign up using the **/api/v1/auth/signup** API with below payload. It will create the user with email id and role as CUSTOMER.

       {
        	"email": "naveenkarhana29@gmail.com",
        	"firstName": "Naveen",
        	"lastName": "Kumar",
        	"password": "Temp123",
        	"confirmPassword": "Temp123",
        	"mobileNumber": "123456789",
        	"role": "ROLE_CUSTOMER"
        }
       
 

> Now Sign in using this user email and password and get the JWT token using 
> **/api/v1/auth/signin** API

    {
    	"email": "naveenkarhana29@gmail.com",
    	"password": "Temp123"
    }

This will result in JWT token in response like below.

    {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYXZlZW5rYXJoYW5hMjlAZ21haWwuY29tIiwiaWF0IjoxNzA5NjU1NjQ0LCJleHAiOjE3MDk3NDIwNDR9.5kqxxVIx9C4Qe41BSMSdJOfQYtX8LPXwteX4-xUIoZQ", 
    "role": "ROLE_CUSTOMER"
    }
    
 2. Sign Up and Sign In of the Event Manager

> Signup and Sign In using the same API with different Email ID and role as ROLE_EVENT_MANAGER

       {
        	"email": "eventmanager@gmail.com",
        	"firstName": "Event",
        	"lastName": "Manager",
        	"password": "Temp123",
        	"confirmPassword": "Temp123",
        	"mobileNumber": "123456789",
        	"role": "ROLE_EVENT_MANAGER"
        }
Then Sign In using /api/v1/auth/signin API using the event manager email id.

    {"email":"eventmanager@gmail.com","password":"Temp123"}

This will result in another JWT Token

    {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJldmVudG1hbmFnZXJAZ21haWwuY29tIiwiaWF0IjoxNzA5NjU2MDE0LCJleHAiOjE3MDk3NDI0MTR9.8IgI4ZxqUkkzx8t0eMeTvBAeMLe0xvxksusjZ2ht9sI",
    "role": "ROLE_EVENT_MANAGER
    }

Now we will use these 2 tokens for further API invocation linked with User and Event Manager.


## Managing  the Events
As managing the events is the responsibility of Event Manager, hence we will use the Event Manager JWT Token to create the event first.

 1. Create the Event
 

> Invoke the /api/v1/events API to create the event with event manager  token, We have to pass this token in **Authorization** header with **Bearer** prefix.


    {    
        "name": "Cutural Show",
        "venue": "Singapore",
        "eventDateTime": "2024-03-07T13:48:18Z",
        "ticketPrice": 200.0,
        "totalTickets":120,
        "availableTickets": 120,
        "eventType": "MUSIC_CONCERT",
        "eventState": "ACTIVE"
    }

This will create the Event with event ID.

    {
        "id": 9,
        "name": "Cutural Show",
        "venue": "Singapore",
        "eventDateTime": "2024-03-07T13:48:18.000+00:00",
        "ticketPrice": 200.0,
        "totalTickets": 120,
        "availableTickets": 120,
        "eventType": "MUSIC_CONCERT",
        "eventState": "ACTIVE",
        "createdBy": "eventmanager@gmail.com",
        "createdDateTime": "2024-03-05T22:09:40.000+00:00",
        "lastModifiedDateTime": "2024-03-05T22:09:40.000+00:00"
    }

## Make a Booking of this Event from Customer

> A user profile can be get using the the API - /api/v1/users/profile

By default any user signed up will have 1000 in the account wallet.

    {
        "id": 17,
        "firstName": "Naveen",
        "lastName": "Kumar",
        "mobileNumber": 123456789,
        "email": "naveenkarhana29@gmail.com",
        "role": "ROLE_CUSTOMER",
        "walletDetails": {
            "walletId": 54,
            "balance": 1000
        }
    }

2. Make the Booking for the event ID 9 which has been created above using the API 

> /api/v1/bookings

    {
    	"bookingUserEmail": "naveenkarhana29@gmail.com",
    	"bookingMobileNumber": 992352,
    	"eventId": 9,
    	"numberOfTickets": 2,
    	"transactionDetail": {
    		"paymentMethod": "WALLET"
    	}
    }
    
This will give the response of booking with booking Id and transaction details like below.

    {
        "bookingId": "88260175-e761-4ac4-bb8a-71a09ebc3d30",
        "bookingUserEmail": "naveenkarhana29@gmail.com",
        "bookingMobileNumber": 992352,
        "bookedBy": "naveenkarhana29@gmail.com",
        "bookingStatus": "ACCEPTED",
        "eventId": 9,
        "numberOfTickets": 2,
        "bookingDateTime": "2024-03-05T16:48:32.608+00:00",
        "eventDetails": {
            "name": "Cutural Show",
            "venue": "Singapore",
            "eventDateTime": "2024-03-07T13:48:18.000+00:00",
            "eventType": "MUSIC_CONCERT"
        },
        "transactionDetail": {
            "transactionId": "83a0d875-8b95-499f-b6c8-493527d6635c",
            "amount": 400.0,
            "transactionStatus": "COMPLETED",
            "paymentMethod": "WALLET",
            "transactionDateTime": "2024-03-05T16:48:32.606+00:00",
            "transactionUpdateDateTime": "2024-03-05T16:48:32.614+00:00"
        },
        "createdDateTime": "2024-03-05T16:48:32.608+00:00",
        "lastModifiedDateTime": "2024-03-05T16:48:32.608+00:00"
    }

Using this way, booking will be confirmed for the event.

Now if you get the Event with /api/v1/events/9 to get the event available tickets (availableTickets) field, it will be subtracted by number of tickets booked.

    {
        "id": 9,
        "name": "Cutural Show",
        "venue": "Singapore",
        "eventDateTime": "2024-03-07T13:48:18.000+00:00",
        "ticketPrice": 200.0,
        "totalTickets": 120,
        "availableTickets": 118,
        "eventType": "MUSIC_CONCERT",
        "eventState": "ACTIVE",
        "createdBy": "eventmanager@gmail.com",
        "createdDateTime": "2024-03-05T22:09:40.000+00:00",
        "lastModifiedDateTime": "2024-03-05T22:18:32.000+00:00"
    }

User Booking history can be fetched using the API **/api/v1/users/bookings**.

    [
        {
            "bookingId": "88260175-e761-4ac4-bb8a-71a09ebc3d30",
            "bookingUserEmail": "naveenkarhana29@gmail.com",
            "bookingMobileNumber": 992352,
            "bookedBy": "naveenkarhana29@gmail.com",
            "bookingStatus": "ACCEPTED",
            "eventId": 9,
            "numberOfTickets": 2,
            "bookingDateTime": "2024-03-05T16:48:32.608+00:00",
            "transactionDetail": {
                "transactionId": "83a0d875-8b95-499f-b6c8-493527d6635c",
                "amount": 400.0,
                "transactionStatus": "COMPLETED",
                "paymentMethod": "WALLET",
                "transactionDateTime": "2024-03-05T22:18:32.000+00:00",
                "transactionUpdateDateTime": "2024-03-05T22:18:32.000+00:00"
            },
            "createdDateTime": "2024-03-05T22:18:32.000+00:00",
            "lastModifiedDateTime": "2024-03-05T22:18:32.000+00:00"
        }
    ]

On Ticket confirmation email will be also sent on bookingUserEmail if SMTP is configured properly.

    Email Template
    
    Event Booking Confirmation
    Dear,
    
    We're thrilled to confirm your booking for the upcoming event:
    
    Event Name: Comedy Show
    
    Date: Mar 7, 2024, 7:18:18 PM
    
    Location: Singapore
    
    Please find the details of your booking below:
    
    Booking ID: 20ceb1d9-eecb-482c-9189-0ca712102997
    
    
    Number of Tickets: 1
    
    Thank you for choosing to attend our event. We look forward to seeing you there!
    
    Best regards,
    
    The Event Management Team

