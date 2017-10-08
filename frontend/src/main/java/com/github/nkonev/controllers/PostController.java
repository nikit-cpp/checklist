package com.github.nkonev.controllers;

import com.github.nkonev.Constants;
import com.github.nkonev.dto.PostDTOExtended;
import com.github.nkonev.repo.jpa.CommentRepository;
import com.github.nkonev.utils.PageUtils;
import com.github.nkonev.converter.PostConverter;
import com.github.nkonev.dto.PostDTO;
import com.github.nkonev.dto.PostDTOWithAuthorization;
import com.github.nkonev.dto.UserAccountDetailsDTO;
import com.github.nkonev.entity.jpa.Post;
import com.github.nkonev.entity.jpa.UserAccount;
import com.github.nkonev.exception.BadRequestException;
import com.github.nkonev.repo.jpa.PostRepository;
import com.github.nkonev.repo.jpa.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@RestController
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PostConverter postConverter;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${custom.postgres.fulltext.reg-config}")
    private String regConfig;

    @GetMapping(Constants.Uls.API+Constants.Uls.POST)
    public List<PostDTO> getPosts(
            @RequestParam(value = "page", required=false, defaultValue = "0") int page,
            @RequestParam(value = "size", required=false, defaultValue = "0") int size,
            @RequestParam(value = "searchString", required=false, defaultValue = "") String searchString
    ) {
        page = PageUtils.fixPage(page);
        size = PageUtils.fixSize(size);
        searchString = StringUtils.trimWhitespace(searchString);

        Map<String, Object> params = new HashMap<>();
        params.put("search", searchString);
        params.put("offset", PageUtils.getOffset(page, size));
        params.put("limit", size);

        List<PostDTO> posts;

        final RowMapper<PostDTO> rowMapper = (resultSet, i) -> new PostDTO(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("text_column"),
                resultSet.getString("title_img")
        );

        if (StringUtils.isEmpty(searchString)) {
            posts = jdbcTemplate.query(
                    "  select id, title, text_no_tags as text_column, title_img \n" +
                     "  from posts.post \n" +
                     "  order by id desc " +
                     "limit :limit offset :offset\n",
                    params,
                    rowMapper
            );
        } else {
            posts = jdbcTemplate.query(
                       "with tsq as (select plainto_tsquery("+regConfig+", :search)) \n" +
                            "select\n" +
                            " id, \n" +
                            " ts_headline("+regConfig+", title, (select * from tsq), 'StartSel=\"<u>\", StopSel=\"</u>\"') as title, \n" +
                            " ts_headline("+regConfig+", text_no_tags, (select * from tsq), 'StartSel=\"<b>\", StopSel=\"</b>\"') as text_column, \n" +
                            " title_img\n" +
                            "from (\n" +
                            "  select id, title, text_no_tags, title_img \n" +
                            "  from posts.post \n" +
                            "  where to_tsvector("+regConfig+", title || ' ' || text_no_tags) @@ (select * from tsq) order by id desc " +
                            "limit :limit offset :offset\n" +
                            ") as foo;",
                    params,
                    rowMapper
            );
        }

        return posts;
    }

    @GetMapping(Constants.Uls.API+Constants.Uls.POST+Constants.Uls.POST_ID)
    public PostDTOExtended getPost(
            @PathVariable(Constants.PathVariables.POST_ID) long id,
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount // null if not authenticated
    ) {
        return postRepository
                .findById(id)
                .map(post -> postConverter.convertToDtoExtended(post, userAccount))
                .orElseThrow(()-> new RuntimeException("Post " + id + " not found"));
    }


    // ================================================= secured

    @PreAuthorize("isAuthenticated()")
    @GetMapping(Constants.Uls.API+Constants.Uls.POST+Constants.Uls.MY)
    public List<PostDTO> getMyPosts(
            @RequestParam(value = "page", required=false, defaultValue = "0") int page,
            @RequestParam(value = "size", required=false, defaultValue = "0") int size,
            @RequestParam(value = "searchString", required=false, defaultValue = "") String searchString // TODO implement
    ) {

        PageRequest springDataPage = new PageRequest(PageUtils.fixPage(page), PageUtils.fixSize(size));

        return postRepository
                .findMyPosts(springDataPage).getContent()
                .stream()
                .map(postConverter::convertToPostDTOWithCleanTags)
                .collect(Collectors.toList());
    }

    // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#el-common-built-in
    @PreAuthorize("isAuthenticated()")
    @PostMapping(Constants.Uls.API+Constants.Uls.POST)
    public PostDTOWithAuthorization addPost(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // null if not authenticated
            @RequestBody @NotNull PostDTO postDTO
    ) {
        Assert.notNull(userAccount, "UserAccountDetailsDTO can't be null");
        if (postDTO.getId()!=0){
            throw new BadRequestException("id cannot be set");
        }
        Post fromWeb = postConverter.convertToPost(postDTO, null);
        UserAccount ua = userAccountRepository.findOne(userAccount.getId()); // Hibernate caches it
        Assert.notNull(ua, "User account not found");
        fromWeb.setOwner(ua);
        Post saved = postRepository.save(fromWeb);
        return postConverter.convertToDto(saved, userAccount);
    }

    @PreAuthorize("@blogSecurityService.hasPostPermission(#postDTO, #userAccount, T(com.github.nkonev.entity.jpa.Permissions).EDIT)")
    @PutMapping(Constants.Uls.API+Constants.Uls.POST)
    public PostDTOWithAuthorization updatePost(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // null if not authenticated
            @RequestBody @NotNull PostDTO postDTO
    ) {
        Assert.notNull(userAccount, "UserAccountDetailsDTO can't be null");
        Post found = postRepository.findOne(postDTO.getId());
        Assert.notNull(found, "Post with id " + postDTO.getId() + " not found");
        Post updatedEntity = postConverter.convertToPost(postDTO, found);
        Post saved = postRepository.save(updatedEntity);
        return postConverter.convertToDto(saved, userAccount);
    }

    @PreAuthorize("@blogSecurityService.hasPostPermission(#postId, #userAccount, T(com.github.nkonev.entity.jpa.Permissions).DELETE)")
    @DeleteMapping(Constants.Uls.API+Constants.Uls.POST+Constants.Uls.POST_ID)
    public void deletePost(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // null if not authenticated
            @PathVariable(Constants.PathVariables.POST_ID) long postId
    ) {
        Assert.notNull(userAccount, "UserAccountDetailsDTO can't be null");
        commentRepository.deleteByPostId(postId);
        postRepository.delete(postId);
        postRepository.flush();
    }
}