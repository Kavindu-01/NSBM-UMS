package lk.nsbm.university.service;

import lk.nsbm.university.entity.BoardingListing;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.ShuttleNotice;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.BoardingListingRepository;
import lk.nsbm.university.repository.ShuttleNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LogisticsAnnouncementService {

    private final ShuttleNoticeRepository shuttleNoticeRepository;
    private final BoardingListingRepository boardingListingRepository;
    private final MediaStorageService mediaStorageService;

    public LogisticsAnnouncementService(ShuttleNoticeRepository shuttleNoticeRepository,
                                        BoardingListingRepository boardingListingRepository,
                                        MediaStorageService mediaStorageService) {
        this.shuttleNoticeRepository = shuttleNoticeRepository;
        this.boardingListingRepository = boardingListingRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional(readOnly = true)
    public List<ShuttleNotice> getRecentShuttleNotices() {
        return shuttleNoticeRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<BoardingListing> getRecentBoardingListings() {
        List<BoardingListing> listings = boardingListingRepository.findTop20ByOrderByCreatedAtDesc();
        listings.forEach(this::primeBoardingListing);
        return listings;
    }

    public void publishShuttleNotice(User driver,
                                     String message,
                                     String runningTimes,
                                     String routeDescription,
                                     String contactInfo) {
        if (driver == null || driver.getRole() != Role.SHUTTLE_DRIVER) {
            throw new IllegalArgumentException("Only shuttle drivers can post shuttle notices");
        }

        ShuttleNotice notice = new ShuttleNotice();
        notice.setAuthor(driver);
        notice.setMessage(clean(message));
        notice.setRunningTimes(clean(runningTimes));
        notice.setRouteDescription(clean(routeDescription));
        String resolvedContact = StringUtils.hasText(contactInfo) ? contactInfo.trim() : driver.getContactNumber();
        notice.setContactInfo(clean(resolvedContact));

        if (!hasAnyContent(notice.getMessage(), notice.getRunningTimes(), notice.getRouteDescription(), notice.getContactInfo())) {
            throw new IllegalArgumentException("Provide at least one detail before publishing");
        }

        shuttleNoticeRepository.save(notice);
    }

    public void publishBoardingListing(User guardian,
                                       String headline,
                                       String description,
                                       String location,
                                       String rentRange,
                                       Integer availableSlots,
                                       String amenities,
                                       String contactInfo,
                                       List<MultipartFile> photoFiles) {
        if (guardian == null || guardian.getRole() != Role.BOARDING_GUARDIAN) {
            throw new IllegalArgumentException("Only boarding guardians can post listings");
        }

        BoardingListing listing = new BoardingListing();
        listing.setGuardian(guardian);
        listing.setHeadline(clean(headline));
        listing.setDescription(clean(description));
        String resolvedContact = StringUtils.hasText(contactInfo) ? contactInfo.trim() : guardian.getContactNumber();
        listing.setContactInfo(clean(resolvedContact));
        listing.setLocationDescription(clean(location));
        listing.setRentRange(clean(rentRange));
        Integer safeSlots = availableSlots != null && availableSlots > 0 ? availableSlots : null;
        listing.setAvailableSlots(safeSlots);
        listing.setAmenities(clean(amenities));
        listing.setPhotoUrls(storeBoardingPhotos(photoFiles));

        if (!hasAnyContent(listing.getHeadline(),
                listing.getDescription(),
                listing.getContactInfo(),
                listing.getLocationDescription(),
                listing.getRentRange(),
                listing.getAmenities(),
                listing.getAvailableSlots() != null ? listing.getAvailableSlots().toString() : null)
                && listing.getPhotoUrls().isEmpty()) {
            throw new IllegalArgumentException("Share at least one detail or photo before publishing");
        }

        boardingListingRepository.save(listing);
    }

    private List<String> storeBoardingPhotos(List<MultipartFile> photoFiles) {
        if (photoFiles == null || photoFiles.isEmpty()) {
            return List.of();
        }
        List<String> stored = new ArrayList<>();
        for (MultipartFile photo : photoFiles) {
            if (photo == null || photo.isEmpty()) {
                continue;
            }
            String storedPath = mediaStorageService.store(photo, "boarding");
            if (StringUtils.hasText(storedPath)) {
                stored.add(storedPath);
            }
            if (stored.size() >= 6) {
                break;
            }
        }
        return stored;
    }

    private boolean hasAnyContent(String... values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return true;
            }
        }
        return false;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void primeBoardingListing(BoardingListing listing) {
        if (listing == null) {
            return;
        }
        if (listing.getGuardian() != null) {
            listing.getGuardian().getStudentId();
            listing.getGuardian().getFullName();
        }
        if (listing.getPhotoUrls() != null) {
            listing.getPhotoUrls().size();
        }
    }
}
