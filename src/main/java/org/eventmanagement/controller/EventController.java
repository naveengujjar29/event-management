package org.eventmanagement.controller;


import jakarta.validation.Valid;

import java.util.Optional;

import org.eventmanagement.dto.EventDto;
import org.eventmanagement.dto.MessageResponse;
import org.eventmanagement.exception.BadRequestException;
import org.eventmanagement.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/events")
@RestController
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('EVENT_MANAGER')")
    public ResponseEntity<?> createEvent(@RequestBody @Valid EventDto eventDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Input JSON is invalid."));
        }
        Optional<EventDto> savedEventDto = this.eventService.createEvent(eventDto);
        return new ResponseEntity<>(savedEventDto.get(), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('EVENT_MANAGER') or hasRole('TICKET_OFFICER') or hasRole('CUSTOMER')")
    public ResponseEntity<Page<EventDto>> getAllEvents(Pageable pageable) {
        Page<EventDto> page = this.eventService.getEvents(pageable);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasRole('EVENT_MANAGER') or hasRole('TICKET_OFFICER') or hasRole('CUSTOMER')")
    public ResponseEntity<?> getEventsById(@PathVariable long eventId) {
        Optional<EventDto> savedEvent = this.eventService.getEventById(eventId);
        if (savedEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return new ResponseEntity<>(savedEvent.get(), HttpStatus.OK);
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasRole('EVENT_MANAGER')")
    public ResponseEntity<?> updateEvent(@PathVariable long eventId, @RequestBody EventDto eventDto) {
        Optional<EventDto> updatedEvent = this.eventService.updateEvent(eventId, eventDto);
        if (updatedEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return new ResponseEntity<>(updatedEvent.get(), HttpStatus.OK);
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> deleteEvent(@PathVariable long eventId) {
        this.eventService.deleteEvent(eventId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('EVENT_MANAGER')")
    public ResponseEntity<?> cancelEvent(@PathVariable long eventId) throws BadRequestException {
        Optional<EventDto> savedEventDto = this.eventService.cancelEvent(eventId);
        return new ResponseEntity<>(savedEventDto.get(), HttpStatus.OK);
    }


}
