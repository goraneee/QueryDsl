package study.querydsl.service;


import static study.querydsl.entity.QMember.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import study.querydsl.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    Iterable result = userRepository.findAll(
        member.age.between(10, 40).and(member.username.eq("member1")));
}
