package vn.hoidanit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoidanit.authservice.domain.Skill;
import vn.hoidanit.authservice.domain.Subscriber;
import vn.hoidanit.authservice.domain.User;
import vn.hoidanit.authservice.dto.CreateSubscriberDTO;
import vn.hoidanit.authservice.dto.ResSubscriberDTO;
import vn.hoidanit.authservice.dto.UpdateSubscriberSkillsDTO;
import vn.hoidanit.authservice.repository.SkillRepository;
import vn.hoidanit.authservice.repository.SubscriberRepository;
import vn.hoidanit.authservice.repository.UserRepository;
import vn.hoidanit.authservice.util.SecurityUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResSubscriberDTO createSubscriber(CreateSubscriberDTO dto) {
        // Get current user
        String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(dto.getEmail());
        User user = userRepository.findByEmail(currentUserEmail);

        // Check if subscriber already exists
        if (subscriberRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Subscriber with email " + dto.getEmail() + " already exists");
        }

        // Get skills
        List<Skill> skills = skillRepository.findByIdIn(dto.getSkillIds());
        if (skills.isEmpty()) {
            throw new RuntimeException("No valid skills found");
        }

        // Create subscriber
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(dto.getEmail());
        subscriber.setName(dto.getName());
        subscriber.setSkills(skills);
        subscriber.setActive(true);
        if (user != null) {
            subscriber.setUser(user);
        }

        Subscriber saved = subscriberRepository.save(subscriber);
        log.info("Created subscriber: email={}, skills={}", saved.getEmail(), skills.size());

        return mapToDTO(saved);
    }

    @Transactional
    public ResSubscriberDTO updateSkills(Long id, UpdateSubscriberSkillsDTO dto) {
        Subscriber subscriber = subscriberRepository.findByIdWithSkills(id)
                .orElseThrow(() -> new RuntimeException("Subscriber not found"));

        List<Skill> skills = skillRepository.findByIdIn(dto.getSkillIds());
        if (skills.isEmpty()) {
            throw new RuntimeException("No valid skills found");
        }

        subscriber.setSkills(skills);
        Subscriber saved = subscriberRepository.save(subscriber);

        log.info("Updated subscriber skills: id={}, skills={}", id, skills.size());
        return mapToDTO(saved);
    }

    public ResSubscriberDTO getMySubscription() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not logged in"));

        Subscriber subscriber = subscriberRepository.findByEmailWithSkills(email)
                .orElseThrow(() -> new vn.hoidanit.authservice.exception.ResourceNotFoundException("Subscription",
                        "email", email));

        return mapToDTO(subscriber);
    }

    public ResSubscriberDTO getSubscriberById(Long id) {
        Subscriber subscriber = subscriberRepository.findByIdWithSkills(id)
                .orElseThrow(
                        () -> new vn.hoidanit.authservice.exception.ResourceNotFoundException("Subscriber", "id", id));

        return mapToDTO(subscriber);
    }

    @Transactional
    public void deleteSubscriber(Long id) {
        if (!subscriberRepository.existsById(id)) {
            throw new vn.hoidanit.authservice.exception.ResourceNotFoundException("Subscriber", "id", id);
        }
        subscriberRepository.deleteById(id);
        log.info("Deleted subscriber: id={}", id);
    }

    public List<Subscriber> findMatchingSubscribers(List<Long> skillIds) {
        return subscriberRepository.findActiveSubscribersBySkillIds(skillIds);
    }

    private ResSubscriberDTO mapToDTO(Subscriber subscriber) {
        ResSubscriberDTO dto = new ResSubscriberDTO();
        dto.setId(subscriber.getId());
        dto.setEmail(subscriber.getEmail());
        dto.setName(subscriber.getName());
        dto.setActive(subscriber.isActive());
        dto.setCreatedAt(subscriber.getCreatedAt());
        dto.setUpdatedAt(subscriber.getUpdatedAt());

        if (subscriber.getSkills() != null) {
            List<ResSubscriberDTO.SkillInfo> skillInfos = subscriber.getSkills().stream()
                    .map(skill -> new ResSubscriberDTO.SkillInfo(skill.getId(), skill.getName()))
                    .collect(Collectors.toList());
            dto.setSkills(skillInfos);
        }

        return dto;
    }
}
