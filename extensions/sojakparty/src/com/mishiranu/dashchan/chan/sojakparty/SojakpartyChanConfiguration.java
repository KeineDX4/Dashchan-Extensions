package com.mishiranu.dashchan.chan.sojakparty;

import android.content.res.Resources;

import chan.content.ChanConfiguration;

public class SojakpartyChanConfiguration extends ChanConfiguration {

	public static final String CAPTCHA_TYPE_NONE = "None";

	public SojakpartyChanConfiguration() {
		request(OPTION_READ_POSTS_COUNT);
		addCaptchaType(CAPTCHA_TYPE_NONE);
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
		setDefaultName("Chud");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		boolean namesAndEmails = !"layer".equals(boardName);
		posting.allowName = namesAndEmails;
		posting.allowTripcode = namesAndEmails;
		posting.allowEmail = namesAndEmails;
		posting.allowSubject = true;
		posting.optionSage = namesAndEmails;
		posting.attachmentCount = 4;
		posting.attachmentMimeTypes.add("application/pdf");
		posting.attachmentMimeTypes.add("audio/*");
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentMimeTypes.add("video/mp4");
		posting.attachmentSpoiler = true;
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}

	@Override
	public Reporting obtainReportingConfiguration(String boardName) {
		Reporting reporting = new Reporting();
		reporting.comment = true;
		reporting.multiplePosts = true;
		return reporting;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		Captcha captcha;

		switch (captchaType) {
			case CAPTCHA_TYPE_NONE:
				captcha = new Captcha();
				captcha.title = CAPTCHA_TYPE_NONE;
				captcha.input = Captcha.Input.ALL;
				captcha.validity = Captcha.Validity.IN_BOARD;
				break;
			default:
				captcha = null;
				break;
		}

		return captcha;
	}

	@Override
	public CustomPreference obtainCustomPreferenceConfiguration(String key) {
		return null;
	}
}
