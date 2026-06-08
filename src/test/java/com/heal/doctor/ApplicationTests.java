package com.heal.doctor;

import com.heal.doctor.models.*;
import com.heal.doctor.models.enums.*;
import com.heal.doctor.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CollaboratorProfileRepository collaboratorProfileRepository;

	@Autowired
	private InvitationRepository invitationRepository;

	@Autowired
	private UserNotificationRepository userNotificationRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void checkCollaboratorIds() {
		System.out.println(">>> CHECKING COLLABORATOR IDs IN DATABASE <<<");

		List<UserEntity> allUsers = userRepository.findAll();
		int usrUserCount = 0;
		int colUserCount = 0;
		for (UserEntity user : allUsers) {
			if (user.getRolesEnum() == RolesEnum.COLLABORATOR) {
				if (user.getUserId() != null) {
					if (user.getUserId().startsWith("USR-")) {
						usrUserCount++;
						System.out.println("COLLABORATOR User with USR- prefix: ID=" + user.getUserId() + ", Email=" + user.getEmail());
					} else if (user.getUserId().startsWith("COL-")) {
						colUserCount++;
					}
				}
			}
		}

		List<CollaboratorProfileEntity> allProfiles = collaboratorProfileRepository.findAll();
		int usrProfileCount = 0;
		int colProfileCount = 0;
		for (CollaboratorProfileEntity profile : allProfiles) {
			if (profile.getCollaboratorId() != null) {
				if (profile.getCollaboratorId().startsWith("USR-")) {
					usrProfileCount++;
					System.out.println("Collaborator Profile with USR- prefix: ID=" + profile.getCollaboratorId() + ", Email=" + profile.getEmail());
				} else if (profile.getCollaboratorId().startsWith("COL-")) {
					colProfileCount++;
				}
			}
		}

		List<InvitationEntity> allInvitations = invitationRepository.findAll();
		int usrInvitationCount = 0;
		int colInvitationCount = 0;
		for (InvitationEntity inv : allInvitations) {
			if (inv.getCollaboratorId() != null) {
				if (inv.getCollaboratorId().startsWith("USR-")) {
					usrInvitationCount++;
					System.out.println("Invitation with USR- collaboratorId: ID=" + inv.getInvitationId() + ", Email=" + inv.getEmail() + ", CollabId=" + inv.getCollaboratorId());
				} else if (inv.getCollaboratorId().startsWith("COL-")) {
					colInvitationCount++;
				}
			}
		}

		List<UserNotification> allUserNotifications = userNotificationRepository.findAll();
		int usrUserNotificationCount = 0;
		int colUserNotificationCount = 0;
		for (UserNotification un : allUserNotifications) {
			if (un.getUserId() != null) {
				if (un.getUserId().startsWith("USR-")) {
					// Check if this userId is a collaborator
					final String uid = un.getUserId();
					boolean isCollab = allUsers.stream()
							.anyMatch(u -> uid.equals(u.getUserId()) && u.getRolesEnum() == RolesEnum.COLLABORATOR);
					if (isCollab) {
						usrUserNotificationCount++;
					}
				} else if (un.getUserId().startsWith("COL-")) {
					colUserNotificationCount++;
				}
			}
		}

		List<NotificationEntity> allNotifications = notificationRepository.findAll();
		int usrSenderNotificationCount = 0;
		int colSenderNotificationCount = 0;
		for (NotificationEntity n : allNotifications) {
			if (n.getSenderId() != null) {
				if (n.getSenderId().startsWith("USR-")) {
					final String sid = n.getSenderId();
					boolean isCollab = allUsers.stream()
							.anyMatch(u -> sid.equals(u.getUserId()) && u.getRolesEnum() == RolesEnum.COLLABORATOR);
					if (isCollab) {
						usrSenderNotificationCount++;
					}
				} else if (n.getSenderId().startsWith("COL-")) {
					colSenderNotificationCount++;
				}
			}
		}

		System.out.println(">>> CHECK SUMMARY <<<");
		System.out.println("Collaborator Users: USR-=" + usrUserCount + ", COL-=" + colUserCount);
		System.out.println("Collaborator Profiles: USR-=" + usrProfileCount + ", COL-=" + colProfileCount);
		System.out.println("Invitations: USR-=" + usrInvitationCount + ", COL-=" + colInvitationCount);
		System.out.println("User Notifications (Collaborators only): USR-=" + usrUserNotificationCount + ", COL-=" + colUserNotificationCount);
		System.out.println("Notifications Sender (Collaborators only): USR-=" + usrSenderNotificationCount + ", COL-=" + colSenderNotificationCount);
		System.out.println(">>> CHECK COMPLETE <<<");
	}

}
