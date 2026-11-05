package com.seohamin.galae.domain.group.dto.message;

import com.seohamin.galae.domain.group.entity.message.GroupMessage;
import com.seohamin.galae.domain.user.dto.UserSimpleResponseDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 그룹 대화: 한 질문에 대한 그룹원의 선택·이유와 메시지
 * 내가 답하기 전(locked)에는 다른 사람 글의 선택과 내용을 주지 않는다.
 * 프론트는 이때 글을 흐리게 보여 주고 답하기 버튼을 띄운다
 *
 * 증분 폴링: 처음에는 after 없이 전체를 받고, 그다음부터는 lastMessageId를 after로 넘겨 새 메시지만 받는다.
 * locked가 true에서 false로 바뀌면(내가 답함) 이미 받은 글의 내용이 열리므로 after 없이 다시 받는다.
 */
@Getter
public class GroupMessageListResponseDto {

    private final Long groupId;
    private final String questionId;
    private final Boolean locked;
    //이 질문을 나눈 멤버 수 (after와 상관없이 전체 기준)
    private final Long sharedMemberCount;
    //이번에 받은 메시지 (after가 있으면 그보다 새로운 것만)
    private final List<Item> messages;
    //다음 폴링에 after로 넘길 값. 새 메시지가 없으면 요청한 after 그대로, 메시지가 하나도 없으면 0
    private final Long lastMessageId;

    public GroupMessageListResponseDto(
            final Long groupId,
            final String questionId,
            final boolean locked,
            final long sharedMemberCount,
            final List<Item> messages,
            final long lastMessageId
    ) {
        this.groupId = groupId;
        this.questionId = questionId;
        this.locked = locked;
        this.sharedMemberCount = sharedMemberCount;
        this.messages = messages;
        this.lastMessageId = lastMessageId;
    }

    @Getter
    public static class Item {
        private final Long id;
        private final UserSimpleResponseDto author;
        private final Boolean mine;
        //공유 메시지(선택과 이유)인지
        private final Boolean share;
        //공유 메시지의 선택. 일반 메시지거나 잠겨 있으면 null
        private final Integer choiceId;
        private final String choiceShortText;
        //잠겨 있으면 null
        private final String text;
        private final LocalDateTime createdAt;

        public Item(final GroupMessage message, final Long viewerId, final boolean locked) {
            final boolean mine = message.getAuthor().getId().equals(viewerId);
            final boolean hidden = locked && !mine;

            this.id = message.getId();
            this.author = new UserSimpleResponseDto(message.getAuthor());
            this.mine = mine;
            this.share = message.isShare();
            this.choiceId = hidden || !message.isShare() ? null : message.getChoice().getChoiceIndex();
            this.choiceShortText = hidden || !message.isShare() ? null : message.getChoice().getShortText();
            this.text = hidden ? null : message.getText();
            this.createdAt = message.getCreatedAt();
        }
    }
}
