package com.stolink.backend.domain.bookmark.service;

import com.stolink.backend.domain.bookmark.dto.BookmarkResponse;
import com.stolink.backend.domain.bookmark.dto.ReadingProgressResponse;
import com.stolink.backend.domain.bookmark.dto.SaveBookmarkRequest;
import com.stolink.backend.domain.bookmark.entity.Bookmark;
import com.stolink.backend.domain.bookmark.repository.BookmarkRepository;
import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final WorkRepository workRepository;

    public Optional<BookmarkResponse> getBookmark(UUID userId, UUID chapterId) {
        return bookmarkRepository.findByUserIdAndChapterId(userId, chapterId)
                .map(BookmarkResponse::from);
    }

    @Transactional
    public BookmarkResponse saveBookmark(UUID userId, UUID chapterId, SaveBookmarkRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        Bookmark bookmark = bookmarkRepository.findByUserIdAndChapterId(userId, chapterId)
                .orElseGet(() -> Bookmark.builder()
                        .user(user)
                        .chapter(chapter)
                        .build());

        bookmark.updateScrollPosition(request.getScrollPosition() != null ? request.getScrollPosition() : 0);

        Bookmark saved = bookmarkRepository.save(bookmark);
        return BookmarkResponse.from(saved);
    }

    public ReadingProgressResponse getReadingProgress(UUID userId, UUID workId) {
        if (!workRepository.existsById(workId)) {
            throw new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId);
        }

        int totalChapters = chapterRepository.countByWorkId(workId);
        int readChapters = bookmarkRepository.countReadChaptersByUserIdAndWorkId(userId, workId);

        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdAndWorkIdOrderByUpdatedAtDesc(userId, workId);

        if (bookmarks.isEmpty()) {
            return ReadingProgressResponse.of(workId, null, null, null, null, totalChapters, 0);
        }

        Bookmark lastBookmark = bookmarks.get(0);
        return ReadingProgressResponse.of(
                workId,
                lastBookmark.getChapter().getId(),
                lastBookmark.getChapter().getTitle(),
                lastBookmark.getChapter().getChapterNumber(),
                lastBookmark.getScrollPosition(),
                totalChapters,
                readChapters
        );
    }
}
