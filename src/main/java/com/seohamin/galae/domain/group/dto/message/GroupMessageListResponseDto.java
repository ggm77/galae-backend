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
 */
@Getter
public class GroupMessageListResponseDto {

    private final Long groupId;
    private final String questionId;
    private final Boolean locked;
    //이 질문을 나눈 멤버 수
    private final Long sharedMemberCount;
    private final List<Item> messages;

    public GroupMessageListResponseDto(
            final Long groupId,
            final String questionId,
            final boolean locked,
            final long sharedMemberCount,
            final List<Item> messages
    ) {
        this.groupId = groupId;
        this.questionId = questionId;
        this.locked = locked;
        this.sharedMemberCount = sharedMemberCount;
        this.messages = messages;
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
