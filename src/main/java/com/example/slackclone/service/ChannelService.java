package com.example.slackclone.service;

import com.example.slackclone.domain.Channel;
import com.example.slackclone.domain.ChannelMember;
import com.example.slackclone.domain.ChannelMemberId;
import com.example.slackclone.domain.ChannelMemberRepository;
import com.example.slackclone.domain.ChannelRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@Service
public class ChannelService {

    /** 목록의 한 줄: 채널과 "내가 참여했는지". */
    public record ChannelView(Channel channel, boolean joined) {
    }

    private final ChannelRepository channels;
    private final ChannelMemberRepository members;
    private final Clock clock;

    public ChannelService(ChannelRepository channels, ChannelMemberRepository members, Clock clock) {
        this.channels = channels;
        this.members = members;
        this.clock = clock;
    }

    // 왜: 만든 사람이 자기 채널에 못 들어가는 일이 없도록 생성과 참여를 한 트랜잭션으로 묶는다.
    // 어디: 이름 중복은 사전 검사 + DB 유니크 제약(동시 요청)으로 막는다.
    @Transactional
    public Channel create(String name, Long userId) {
        if (channels.existsByName(name)) {
            throw new DuplicateChannelNameException(name);
        }
        Channel channel;
        try {
            channel = channels.saveAndFlush(new Channel(name, userId, clock.instant()));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateChannelNameException(name);
        }
        members.save(new ChannelMember(channel.getId(), userId, clock.instant()));
        return channel;
    }

    // 왜: 이미 참여한 사람이 다시 눌러도 오류 없이 같은 결과가 되게(멱등) 한다.
    // 어디: 동시에 두 번 눌러 복합 PK가 충돌해도 이미 참여한 것으로 본다.
    public Channel join(Long channelId, Long userId) {
        Channel channel = channels.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
        if (!members.existsById(new ChannelMemberId(channelId, userId))) {
            try {
                members.saveAndFlush(new ChannelMember(channelId, userId, clock.instant()));
            } catch (DataIntegrityViolationException ignored) {
                // 동시에 참여한 경우: 이미 참여 상태이므로 그대로 둔다.
            }
        }
        return channel;
    }

    @Transactional(readOnly = true)
    public List<ChannelView> list(Long userId) {
        Set<Long> joined = members.findChannelIdsByUserId(userId);
        return channels.findAll().stream()
                .map(c -> new ChannelView(c, joined.contains(c.getId())))
                .toList();
    }

    // 왜: 참여자만 채널에 접근하는 규칙을 한 곳에 둔다. 4단계 메시지도 이 메서드를 쓴다.
    // 어디: 없는 채널은 404, 미참여는 403 예외.
    @Transactional(readOnly = true)
    public Channel requireMember(Long channelId, Long userId) {
        Channel channel = channels.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
        if (!members.existsById(new ChannelMemberId(channelId, userId))) {
            throw new NotChannelMemberException(channelId);
        }
        return channel;
    }
}
