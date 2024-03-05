package org.eventmanagement.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eventmanagement.converter.ObjectConverter;
import org.eventmanagement.dto.BookingDto;
import org.eventmanagement.dto.BookingEventDetailsDto;
import org.eventmanagement.dto.EmailDetailsDto;
import org.eventmanagement.dto.PaymentMethod;
import org.eventmanagement.dto.TransactionStatus;
import org.eventmanagement.dto.UserDetailsImpl;
import org.eventmanagement.enums.BookingStatus;
import org.eventmanagement.enums.Role;
import org.eventmanagement.exception.BadRequestException;
import org.eventmanagement.exception.EntityDoesNotExistException;
import org.eventmanagement.model.Booking;
import org.eventmanagement.model.Event;
import org.eventmanagement.model.User;
import org.eventmanagement.model.Wallet;
import org.eventmanagement.repository.BookingRepository;
import org.eventmanagement.repository.EventRepository;
import org.eventmanagement.repository.UserRepository;
import org.eventmanagement.repository.WalletRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectConverter objectConverter;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private EmailService emailService;

    @Transactional(rollbackFor = {Exception.class})
    public Optional<BookingDto> bookEventTicket(BookingDto bookingDto) throws EntityDoesNotExistException,
            BadRequestException {
        LOGGER.debug("Ticket booking has been started.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String whoAmI = "";
        Role role = Role.ROLE_CUSTOMER;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            whoAmI = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
            role = ((UserDetailsImpl) authentication.getPrincipal()).getRole();
        }

        if (role.equals(Role.ROLE_CUSTOMER) && bookingDto.getTransactionDetail().getPaymentMethod().equals(PaymentMethod.CASH)) {
            throw new BadRequestException("CUSTOMER can not purchase the tickets with Payment Method CASH.");
        }

        LOGGER.debug("User " + whoAmI + " is performing the booking operation with role " + role.toString());
        // First check for Events validity, exist or not?
        validateEvent(bookingDto.getEventId(), role);

        //Validate if we have sufficient number of tickets or not for this event
        checkForAvailableTickets(bookingDto.getEventId(), bookingDto.getNumberOfTickets());

        //Validate if User has the required amount in Wallet
        checkForWalletBalanceIfWalletPaymentIsUsed(bookingDto, role, whoAmI);

        bookingByUPIOrCard(bookingDto);

        // set the identity from JWT token who is performing this booking.
        bookingDto.setBookedBy(whoAmI);

        //populateBookingUserDetails
        populateBookingUserDetails(bookingDto, role, whoAmI);

        Event event =
                this.eventRepository.findById(bookingDto.getEventId()).orElseThrow(() -> new EntityDoesNotExistException("Event ID does not exist"));
        event.setAvailableTickets(event.getAvailableTickets() - bookingDto.getNumberOfTickets());
        BookingEventDetailsDto bookingEventDetailsDto = (BookingEventDetailsDto) this.objectConverter.convert(event,
                BookingEventDetailsDto.class);
        Booking booking = (Booking) this.objectConverter.convert(bookingDto, Booking.class);
        booking.getTransactionDetail().setBooking(booking);
        Booking savedBooking = this.bookingRepository.saveAndFlush(booking);
        this.eventRepository.saveAndFlush(event);
        BookingDto savedBookingDto = (BookingDto) this.objectConverter.convert(savedBooking, BookingDto.class);
        savedBookingDto.setEventDetails(bookingEventDetailsDto);
        if (savedBookingDto.getBookingStatus().equals(BookingStatus.ACCEPTED)) {
            sendMailOfBooking(savedBookingDto);
        }
        return Optional.of(savedBookingDto);
    }

    private void bookingByUPIOrCard(BookingDto bookingDto) {
        if (bookingDto.getTransactionDetail().getPaymentMethod().equals(PaymentMethod.UPI) || bookingDto.getTransactionDetail().getPaymentMethod().equals(PaymentMethod.CARD)) {
            bookingDto.getTransactionDetail().setTransactionStatus(TransactionStatus.PENDING);
            bookingDto.setBookingStatus(BookingStatus.PENDING);
        }
    }

    private void checkForWalletBalanceIfWalletPaymentIsUsed(BookingDto dto, Role role, String whoAmI) throws EntityDoesNotExistException, BadRequestException {
        if (role.equals(Role.ROLE_CUSTOMER) && dto.getTransactionDetail().getPaymentMethod().equals(PaymentMethod.WALLET)) {
            User savedUser = this.userRepository.findByEmail(whoAmI).orElseThrow(() -> new EntityDoesNotExistException(
                    "User does not exist."));
            Event event =
                    this.eventRepository.findById(dto.getEventId()).orElseThrow(() -> new EntityDoesNotExistException(
                            "Event does not exist"));
            double bookingAmount = dto.getNumberOfTickets() * event.getTicketPrice();

            Optional<Wallet> wallet = this.walletRepository.findByUserId(savedUser.getId());

            if (wallet.isEmpty()) {
                throw new EntityDoesNotExistException("Can't book with wallet as no wallet exist for this user.");
            }

            if (wallet.get().getBalance() < bookingAmount) {
                throw new BadRequestException("You can not proceed with booking as wallet amount is lower than " +
                        "booking amount." + "Booking amount is " + bookingAmount + " where as wallet amount is " + wallet.get().getBalance());
            } else {
                LOGGER.debug("Wallet has the required amount, marking ");
                double updatedWalletBalance = wallet.get().getBalance() - bookingAmount;
                wallet.get().setBalance(updatedWalletBalance);
                this.walletRepository.saveAndFlush(wallet.get());
                dto.getTransactionDetail().setTransactionStatus(TransactionStatus.COMPLETED);
                dto.setBookingStatus(BookingStatus.ACCEPTED);
                dto.getTransactionDetail().setAmount(bookingAmount);
            }
        }
    }

    private void populateBookingUserDetails(BookingDto bookingDto, Role role, String whoAmI) throws EntityDoesNotExistException, BadRequestException {

        if (role.equals(Role.ROLE_CUSTOMER)) {
            Optional<User> savedUser = this.userRepository.findByEmail(whoAmI);
            if (savedUser.isEmpty()) {
                LOGGER.error("Unexpected error, user should exist, aborting.");
                throw new EntityDoesNotExistException("User does not exist in system with email id::" + whoAmI);
            }
            LOGGER.debug("Populating Booking user details if not provided by customer, this is only allowed for " +
                    "CUSTOMER role.");
            if (null == bookingDto.getBookingUserEmail()) {
                bookingDto.setBookingUserEmail(savedUser.get().getEmail());
            }
            if (null == bookingDto.getBookingMobileNumber()) {
                //TODO add mobile number support //bookingDto.setBookingUserEmail(savedUser.get().PhoneNumber());
            }
        } else if (role.equals(Role.ROLE_TICKET_OFFICER)) {
            if (null == bookingDto.getBookingUserEmail() || null == bookingDto.getBookingMobileNumber()) {
                throw new BadRequestException("Booking user email and mobile number is mandatory to provide.");
            }
        }
    }

    private void checkForAvailableTickets(long eventId, int numberOfTickets) {
        Optional<Event> event = this.eventRepository.findById(eventId);
        if (event.isPresent()) {
            if (event.get().getAvailableTickets() < numberOfTickets) {
                throw new IllegalStateException("Not enough tickets available for this event.");
            }

        }
    }

    private void validateEvent(long eventId, Role role) throws EntityDoesNotExistException, BadRequestException {
        Optional<Event> event = this.eventRepository.findById(eventId);
        if (event.isEmpty()) {
            LOGGER.error("Event does not exist in system with event ID" + eventId);
            throw new EntityDoesNotExistException("Event with this id" + eventId + " does not exist.");
        }
        // This validation is only for customer to book the event under 6 month and before 24 hours.
        // Ticket Officer can issue ticket for such events.
        if (role.equals(Role.ROLE_CUSTOMER)) {
            LOGGER.debug("Validating the event book time for customer role.");
            DateTime target = new DateTime(event.get().getEventDateTime(), DateTimeZone.UTC);
            DateTime currentDateTime = DateTime.now(DateTimeZone.UTC);
            Duration zeroDuration = Duration.ZERO;
            Duration duration = new Duration(currentDateTime, target);
            if (duration.isShorterThan(zeroDuration)) {
                throw new BadRequestException("This event has been passed, you can't book it.");
            }
            Duration twentyFourHours = Duration.standardHours(24);
            Duration sixMonthDuration = Duration.standardDays(180);
            if (duration.isLongerThan(sixMonthDuration)) {
                LOGGER.error("Event " + eventId + " is before 6 months, such event booking is not allowed.");
                throw new BadRequestException("You can book events only under 6 months of duration.");
            }
            if (duration.isShorterThan(twentyFourHours)) {
                LOGGER.error("Event " + eventId + " is under 24 hours, booking now allowed.");
                throw new BadRequestException("You can book events before 24 hour only.");
            }
        }
    }


    public Optional<BookingDto> getBookingDetails(String bookingId) throws EntityDoesNotExistException {
        Booking booking =
                this.bookingRepository.findById(UUID.fromString(bookingId)).orElseThrow(() -> new EntityDoesNotExistException("Entity does not exist for this booking ID" + bookingId));
        BookingDto bookingDto = (BookingDto) this.objectConverter.convert(booking, BookingDto.class);
        return Optional.of(bookingDto);
    }

    public void sendMailOfBooking(BookingDto savedBookingDto) {
        EmailDetailsDto emailDetailDto = new EmailDetailsDto();
        emailDetailDto.setTo(savedBookingDto.getBookingUserEmail());
        emailDetailDto.setSubject("Ticket Confirmation.");
        emailDetailDto.setTemplateName("email-template.ftl");
        // Prepare model for the template
        Map<String, Object> model = new HashMap<>();
        model.put("eventname", savedBookingDto.getEventDetails().getName());
        model.put("eventdate", savedBookingDto.getEventDetails().getEventDateTime());
        model.put("eventvenue", savedBookingDto.getEventDetails().getVenue());
        model.put("bookingid", savedBookingDto.getBookingId());
        model.put("bookingNumTickets", savedBookingDto.getNumberOfTickets());
        emailDetailDto.setModel(model);
        try {
            // Send the email
            emailService.sendEmail(emailDetailDto);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public List<BookingDto> getUserBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String whoAmI = "";
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            whoAmI = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
        }
        List<Booking> bookings = this.bookingRepository.findAllByBookedBy(whoAmI);
        List<BookingDto> savedBookings =
                bookings.stream().map(booking -> (BookingDto) this.objectConverter.convert(booking,
                        BookingDto.class)).collect(Collectors.toList());
        return savedBookings;
    }
}
