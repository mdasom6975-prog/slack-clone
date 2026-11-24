package com.example.slackclone.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    boolean existsByName(String name);
}
