package com.example.demo.config.security;

import com.example.demo.config.security.oauth2.UserPrincipal;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserSearchService userSearchService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userSearchService.findByEmail(username);
		return new UserPrincipal(user);
	}
}
