package com.rebirth.my.mapper;

import com.rebirth.my.domain.OAuthAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface OAuthAccountMapper {
    void save(OAuthAccount oAuthAccount);

    Optional<OAuthAccount> findByProviderAndProviderUserId(@Param("provider") String provider,
            @Param("providerUserId") String providerUserId);

    java.util.List<OAuthAccount> findByUserId(@Param("userId") Long userId);

    int updateUserId(@Param("oldUserId") Long oldUserId, @Param("newUserId") Long newUserId);
}
