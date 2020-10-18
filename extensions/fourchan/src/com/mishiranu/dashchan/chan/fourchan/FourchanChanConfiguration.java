package com.mishiranu.dashchan.chan.fourchan;

import android.content.res.Resources;
import android.util.Pair;
import chan.content.ChanConfiguration;
import chan.content.ChanMarkup;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FourchanChanConfiguration extends ChanConfiguration {
	private static final String KEY_FLAGS_ENABLED = "flags_enabled";
	private static final String KEY_SPOILERS_ENABLED = "spoilers_enabled";
	private static final String KEY_CODE_ENABLED = "code_enabled";
	private static final String KEY_MAX_COMMENT_LENGTH = "max_comment_length";
	private static final String KEY_SAFE_FOR_WORK = "safe_for_work";
	private static final String KEY_REPORT_REASONS = "report_reasons";

	private static final String KEY_MATH_TAGS = "math_tags";

	public FourchanChanConfiguration() {
		request(OPTION_ALLOW_CAPTCHA_PASS);
		setDefaultName("Anonymous");
		setBumpLimit(300);
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
		addCustomPreference(KEY_MATH_TAGS, false);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowCatalog = true;
		board.allowCatalogSearch = true;
		board.allowArchive = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = !StringUtils.isEmpty(get(boardName, KEY_REPORT_REASONS, ""));
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		boolean isCancerBoard = "b".equals(boardName) || "soc".equals(boardName);
		posting.allowName = !isCancerBoard;
		posting.allowTripcode = !isCancerBoard;
		posting.allowEmail = true;
		posting.allowSubject = newThread && !isCancerBoard;
		posting.optionSage = true;
		posting.maxCommentLength = get(boardName, KEY_MAX_COMMENT_LENGTH, 2000);
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentSpoiler = get(boardName, KEY_SPOILERS_ENABLED, false);
		posting.hasCountryFlags = get(boardName, KEY_FLAGS_ENABLED, false);
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
		List<ReportReason> reportReasons = ReportReason.parse(get(boardName, KEY_REPORT_REASONS, ""));
		Reporting reporting = new Reporting();
		for (ReportReason reportReason : reportReasons) {
			reporting.types.add(new Pair<>(reportReason.getKey(), reportReason.title));
		}
		return reporting;
	}

	@Override
	public Authorization obtainCaptchaPassConfiguration() {
		Authorization authorization = new Authorization();
		authorization.fieldsCount = 2;
		authorization.hints = new String[] {"Token", "PIN"};
		return authorization;
	}

	@Override
	public CustomPreference obtainCustomPreferenceConfiguration(String key) {
		if (KEY_MATH_TAGS.equals(key)) {
			Resources resources = getResources();
			CustomPreference customPreference = new CustomPreference();
			customPreference.title = resources.getString(R.string.preference_math_tags);
			customPreference.summary = resources.getString(R.string.preference_math_tags_summary);
			return customPreference;
		}
		return null;
	}

	public boolean isTagSupported(String boardName, int tag) {
		if (tag == ChanMarkup.TAG_SPOILER) {
			return get(boardName, KEY_SPOILERS_ENABLED, false);
		}
		if (tag == ChanMarkup.TAG_CODE) {
			return get(boardName, KEY_CODE_ENABLED, false);
		}
		return false;
	}

	public boolean isMathTagsHandlingEnabled() {
		return get(null, KEY_MATH_TAGS, false);
	}

	public boolean isSafeForWork(String boardName) {
		return get(boardName, KEY_SAFE_FOR_WORK, false);
	}

	public void updateFromBoardsJson(JSONObject jsonObject) {
		try {
			JSONArray jsonArray = jsonObject.getJSONArray("boards");
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObject = jsonArray.getJSONObject(i);
				String boardName = CommonUtils.getJsonString(jsonObject, "board");
				boolean areSpoilersEnabled = jsonObject.optInt("spoilers") != 0;
				boolean isCodeEnabled = jsonObject.optInt("code_tags") != 0;
				boolean areFlagsEnabled = jsonObject.optInt("country_flags") != 0;
				int bumpLimit = jsonObject.optInt("bump_limit");
				int maxCommentLength = jsonObject.optInt("max_comment_chars");
				boolean safeForWork = jsonObject.optInt("ws_board", 0) != 0;
				set(boardName, KEY_SPOILERS_ENABLED, areSpoilersEnabled);
				set(boardName, KEY_CODE_ENABLED, isCodeEnabled);
				set(boardName, KEY_FLAGS_ENABLED, areFlagsEnabled);
				if (bumpLimit != 0) {
					storeBumpLimit(boardName, bumpLimit);
				}
				if (maxCommentLength > 0) {
					set(boardName, KEY_MAX_COMMENT_LENGTH, maxCommentLength);
				}
				set(boardName, KEY_SAFE_FOR_WORK, safeForWork);
			}
		} catch (JSONException e) {
			// Ignore exception
		}
	}

	public void updateReportingConfiguration(String boardName, List<ReportReason> reportReasons) {
		set(boardName, KEY_REPORT_REASONS, ReportReason.serialize(reportReasons));
	}
}
