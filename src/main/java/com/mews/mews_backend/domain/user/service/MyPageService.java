package com.mews.mews_backend.domain.user.service;

import com.mews.mews_backend.api.user.dto.GetMyPageBookmarkRes;
import com.mews.mews_backend.api.user.dto.GetMyPageRes;
import com.mews.mews_backend.api.user.dto.UserDto;
import com.mews.mews_backend.domain.article.entity.Article;
import com.mews.mews_backend.domain.article.repository.ArticleRepository;
import com.mews.mews_backend.domain.user.entity.Bookmark;
import com.mews.mews_backend.domain.user.entity.Like;
import com.mews.mews_backend.domain.user.entity.User;
import com.mews.mews_backend.domain.user.repository.BookmarkRepository;
import com.mews.mews_backend.domain.user.repository.LikeRepository;
import com.mews.mews_backend.domain.user.repository.UserRepository;
import com.mews.mews_backend.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mews.mews_backend.global.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MyPageService {

    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;

    private final LikeRepository likeRepository;

    //프로필
    public GetMyPageRes getUserInfo(Integer userId){
        Optional<User> userResult = userRepository.findById(userId);
        User user = userResult.orElseThrow();

        GetMyPageRes userDto = GetMyPageRes.builder()
                .imgUrl(user.getImgUrl())
                .userName(user.getUserName())
                .introduction(user.getIntroduction())
                .bookmarkCount(user.getBookmarkCount())
                .likeCount(user.getLikeCount())
                .subscribeCount(user.getSubscribeCount())
                .build();

        return userDto;
    }
    //프로필 편집
    public void updateUser(Integer userId, UserDto.updateProfile profile){
        Optional<User> userResult = userRepository.findById(userId );

        userResult.ifPresent(user -> {
            //이름 바꾸기
            if(profile.getUserName()!=null){
                user.changeName(profile.getUserName());
            }
            //이미지 바꾸기
            if(profile.getImgUrl()!=null){
                user.changeImg(profile.getImgUrl());
            }
            //소개 바꾸기
            if(profile.getIntroduction()!=null){
                user.changeIntroduction(profile.getIntroduction());
            }
            //무조건 open 값 받아오기 - true:공개, false:비공개
            user.changeIsOpen(profile.isOpen());

            userRepository.save(user);
        });
    }

    public void USER_VALIDATION(Integer userId){
        //토큰 값의 유저와 userId의 유저가 일치하는지
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(userRepository.findById(userId).orElseThrow().getUserEmail())) {
            throw new BaseException(NOT_AUTHENTICATED_USER);
        }
    }

    public void USER_BOOKMARK_VALIDATION(Integer userId, Integer articleId){
        List<Bookmark> bookmarkValidation = bookmarkRepository.existsByIdAndArticleId(userId, articleId);
        if(!bookmarkValidation.isEmpty()){
            throw new BaseException(USER_BOOKMARK_EXISTS);
        }
    }

    public void USER_LIKE_VALIDATION(Integer userId, Integer articleId){
        List<Like> likeValidation = likeRepository.existsByIdAndArticleId(userId, articleId);
        if(!likeValidation.isEmpty()){
            throw new BaseException(USER_LIKE_EXISTS);
        }
    }

    //북마크 추가
    public void insertBookmark(Integer userId, Integer articleId) {
        log.info("=======예외 처리======");
        //예외 처리 : 토큰 값의 유저와 userId 값이 일치하지 않으면 예외 발생
        USER_VALIDATION(userId);
        //예외 처리 : 이미 북마크된 아티클이면 예외 발생
        USER_BOOKMARK_VALIDATION(userId, articleId);

        //북마크 추가
        User user = userRepository.findById(userId).orElseThrow();
        Article article = articleRepository.findById(articleId).orElseThrow();

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .article(article)
                .build();

        bookmarkRepository.save(bookmark);

        //user bookmarkcnt +1증가
        user.upBookmark();
        userRepository.save(user);
    }

    //내 북마크 글 가져오기
    public List<GetMyPageBookmarkRes> getMyBookmark(Integer userId){
        List<Bookmark> findMyBookmark = bookmarkRepository.findAllByUserId(userId);
        List<GetMyPageBookmarkRes> getMyPageBookmarkRes = new ArrayList<>();

        for(Bookmark bookmark : findMyBookmark){
            GetMyPageBookmarkRes dto = GetMyPageBookmarkRes.builder()
                    .id(bookmark.getArticle().getArticle_id())
                    .title(bookmark.getArticle().getTitle())
                    .likeCount(bookmark.getArticle().getLike_count())
                    .editors("일단 X")
                    .img("일단 X")
                    .build();
            getMyPageBookmarkRes.add(dto);
        }
        return getMyPageBookmarkRes;
    }

    //북마크 취소
    public void deleteBookmark(Integer userId, Integer articleId) {
        User user = userRepository.findById(userId).orElseThrow();
        //북마크 삭제
        bookmarkRepository.deleteByIdAndArticleId(userId, articleId);
        //북마크cnt --
        user.downBookmark();
        userRepository.save(user);
    }

    //좋아요
    public void likeArticle(Integer userId, Integer articleId){
        log.info("=======예외 처리======");
        //예외 처리 : 토큰 값의 유저와 userId 값이 일치하지 않으면 예외 발생
        USER_VALIDATION(userId);
        //예외 처리 : 이미 좋아요된 아티클이면 예외 발생
        USER_LIKE_VALIDATION(userId, articleId);
        //좋아요 추가
        User user = userRepository.findById(userId).orElseThrow();
        Article article = articleRepository.findById(articleId).orElseThrow();

        Like like = Like.builder()
                .user(user)
                .article(article)
                .build();

        likeRepository.save(like);

        //user likecnt +1증가
        user.upLike();
        userRepository.save(user);
    }

    //좋아요 취소
    public void deleteLike(Integer userId, Integer articleId) {
        User user = userRepository.findById(userId).orElseThrow();
        //좋아요 삭제
        likeRepository.deleteByIdAndArticleId(userId, articleId);
        //좋아요 cnt --
        user.downLike();
        userRepository.save(user);
    }

}
