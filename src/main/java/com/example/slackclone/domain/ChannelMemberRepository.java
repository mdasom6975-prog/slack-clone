package com.example.slackclone.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, ChannelMemberId> {

    @Query("select m.channelId from ChannelMember m where m.userId = :userId")
    Set<Long> findChannelIdsByUserId(Long userId);
}
