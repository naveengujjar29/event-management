package org.eventmanagement.service;

import java.util.Optional;

import org.eventmanagement.converter.ObjectConverter;
import org.eventmanagement.dto.EventDto;
import org.eventmanagement.dto.UserDetailsImpl;
import org.eventmanagement.enums.EventState;
import org.eventmanagement.model.Event;
import org.eventmanagement.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventService {

    @Autowired
    private ObjectConverter objectConverter;

    @Autowired
    private EventRepository eventRepository;

    public Optional<EventDto> createEvent(EventDto eventDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = "";
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            createdBy = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        }
        eventDto.setCreatedBy(createdBy);
        Event event = (Event) this.objectConverter.convert(eventDto, Event.class);
        Event savedEvent = this.eventRepository.save(event);
        EventDto returnedEvent = (EventDto) this.objectConverter.convert(savedEvent, EventDto.class);
        return Optional.of(returnedEvent);
    }


    public Page<EventDto> getEvents(Pageable pageable) {
        Page<Event> savedEvents = this.eventRepository.findAll(pageable);
        Page<EventDto> savedEventDTOPage = savedEvents.map(s -> (EventDto) this.objectConverter.convert(s,
                EventDto.class));
        return savedEventDTOPage;
    }


    public Optional<EventDto> getEventById(long eventId) {
        Optional<Event> savedEvent = this.eventRepository.findById(eventId);
        if (savedEvent.isPresent()) {
            EventDto returnedEventDTO = (EventDto) this.objectConverter.convert(savedEvent.get(), EventDto.class);
            return Optional.of(returnedEventDTO);
        }
        return Optional.empty();
    }

    public Optional<EventDto> updateEvent(long id, EventDto eventDto) {
        Optional<Event> savedEvent = this.eventRepository.findById(id);
        if (savedEvent.isPresent()) {
            savedEvent.get().setName(eventDto.getName());
            savedEvent.get().setVenue(eventDto.getName());
            savedEvent.get().setEventType(eventDto.getEventType());
            savedEvent.get().setAvailableTickets(eventDto.getAvailableTickets());
            savedEvent.get().setTicketPrice(eventDto.getTicketPrice());
            Event updatedSavedEvent = this.eventRepository.saveAndFlush(savedEvent.get());
            EventDto savedEventDto = (EventDto) this.objectConverter.convert(updatedSavedEvent, EventDto.class);
            return Optional.of(savedEventDto);
        }
        return Optional.empty();
    }

    public void deleteEvent(long eventId) {
        Optional<Event> savedEvent = this.eventRepository.findById(eventId);
        if (savedEvent.isPresent()) {
            this.eventRepository.delete(savedEvent.get());
        }
    }

    public Optional<EventDto> cancelEvent(long id) {
        Optional<Event> savedEvent = this.eventRepository.findById(id);
        if (savedEvent.isPresent()) {
            savedEvent.get().setEventState(EventState.CANCELLED);
            Event updatedSavedEvent = this.eventRepository.saveAndFlush(savedEvent.get());
            EventDto savedEventDto = (EventDto) this.objectConverter.convert(updatedSavedEvent, EventDto.class);
            return Optional.of(savedEventDto);
        }
        return Optional.empty();
    }


}
