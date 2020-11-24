package com.mishiranu.dashchan.chan.erlach;

import android.annotation.SuppressLint;
import android.net.Uri;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("SimpleDateFormat")
public class ErlachPostsParser {
	private final ErlachChanLocator locator;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final Pattern POST_REFERENCE = Pattern.compile("<a class=\".*?link.*?\".*?href=\"(.*?)\".*?>" +
			"(?:>>|&gt;&gt;)(\\w+)(?:<.*?>)?</a>");
	private static final Pattern POST_LINK_WITH_ID = Pattern.compile("(<a .*?) ?id=\".*?\"(.*?>)");
	private static final Pattern POST_QUOTE = Pattern.compile("(?<!<br>)<span class=\"citate\"");
	private static final Pattern NUMBER = Pattern.compile("\\d+");

	public ErlachPostsParser(Object linked) {
		locator = ErlachChanLocator.get(linked);
	}

	private void closeThread() {
		if (thread != null) {
			thread.setPosts(posts);
			thread.addPostsCount(1);
			threads.add(thread);
			posts.clear();
		}
	}

	public ArrayList<Posts> convertThreads(String source) throws ParseException {
		threads = new ArrayList<>();
		PARSER.parse(source, this);
		closeThread();
		return threads;
	}

	public ArrayList<Post> convertPosts(String source) throws ParseException {
		PARSER.parse(source, this);
		return posts;
	}

	private static final TemplateParser<ErlachPostsParser> PARSER = TemplateParser
			.<ErlachPostsParser>builder()
			.equals("div", "class", "content-title")
			.content((instance, holder, text) -> {
				if (holder.threads == null) {
					holder.post = new Post();
					holder.post.setSubject(StringUtils.clearHtml(text).trim());
				}
			})
			.contains("div", "data-id", "")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post == null) {
					holder.post = new Post();
				}
				String id = holder.locator.convertToDecimalNumber(attributes.get("data-id"));
				if (StringUtils.emptyIfNull(attributes.get("class")).contains("head")) {
					holder.parent = id;
				} else {
					holder.post.setParentPostNumber(holder.parent);
				}
				holder.post.setPostNumber(id);
				holder.post.setSage(StringUtils.emptyIfNull(attributes.get("class")).contains("sage"));
				return false;
			})
			.equals("a", "class", "post-topic")
			.content((instance, holder, text) -> holder.post.setSubject(StringUtils.clearHtml(text).trim()))
			.contains("div", "data-ts", "")
			.open((instance, holder, tagName, attributes) -> {
				holder.post.setTimestamp(Long.parseLong(Objects.requireNonNull(attributes.get("data-ts"))));
				return false;
			})
			.contains("img", "class", "media image")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null) {
					holder.attachment = new FileAttachment();
					String imgSrc = StringUtils.emptyIfNull(attributes.get("src"));
					holder.attachment.setFileUri(holder.locator, Uri.parse(imgSrc));
					holder.attachment.setThumbnailUri(holder.locator,
							Uri.parse(imgSrc.substring(0, imgSrc.length() - 4) + "-R256.jpg"));
					holder.post.setAttachments(holder.attachment);
				}
				return false;
			})
			.name("canvas")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.attachment != null) {
					holder.attachment.setWidth(Integer.parseInt(Objects.requireNonNull(attributes.get("width"))));
					holder.attachment.setHeight(Integer.parseInt(Objects.requireNonNull(attributes.get("height"))));
				}
				return false;
			})
			.equals("div", "class", "omitted")
			.content((instance, holder, text) -> {
				// Ignore this block
			})
			.equals("div", "class", "post-message")
			.content((instance, holder, text) -> {
				if (holder.post != null) {
					// Fix links
					text = StringUtils.replaceAll(text, POST_REFERENCE, matcher ->
							"<a href=\"" + matcher.group(1) + "\">&gt;&gt;" +
							holder.locator.convertToDecimalNumber(matcher.group(2)) + "</a>");
					// Remove IDs from links
					text = StringUtils.replaceAll(text, POST_LINK_WITH_ID, matcher -> matcher.group(1) + matcher.group(2));
					// Make quotes start from new line
					text = StringUtils.replaceAll(text, POST_QUOTE, matcher -> "<br>" + matcher.group());
					holder.post.setComment(text);
					holder.posts.add(holder.post);
					holder.attachment = null;
					holder.post = null;
				}
			})
			.equals("div", "class", "post-header")
			.open((instance, holder, tagName, attributes) -> {
				holder.closeThread();
				holder.thread = new Posts();
				holder.post.setParentPostNumber(null);
				holder.parent = holder.post.getPostNumber();
				return false;
			})
			.contains("span", "class", "info-posts-count")
			.contains("span", "class", "info-images-count")
			.content((instance, holder, text) -> {
				Matcher matcher = NUMBER.matcher(text);
				if (matcher.find()) {
					if (text.contains("<span class=\"en\">I: </span>")) {
						holder.thread.addPostsWithFilesCount(Integer.parseInt(matcher.group()));
					} else {
						holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
					}
				}
			})
			.prepare();
}
