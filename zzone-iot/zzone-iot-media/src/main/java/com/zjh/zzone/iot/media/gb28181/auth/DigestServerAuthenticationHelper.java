package com.zjh.zzone.iot.media.gb28181.auth;

import gov.nist.core.InternalErrorHandler;
import lombok.extern.slf4j.Slf4j;

import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Random;

/**
 * Implements the HTTP digest authentication method server side functionality.
 *
 * @author M. Ranganathan
 * @author Marc Bednarek
 */
@Slf4j
public class DigestServerAuthenticationHelper {

    private final MessageDigest messageDigest;

    public static final String DEFAULT_ALGORITHM = "MD5";
    public static final String DEFAULT_SCHEME = "Digest";

    /** to hex converter */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Default constructor.
     * @throws NoSuchAlgorithmException
     */
    public DigestServerAuthenticationHelper()
            throws NoSuchAlgorithmException {
        messageDigest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
    }

    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    /**
     * Generate the challenge string.
     *
     * @return a generated nonce.
     */
    private String generateNonce() {
        long time = Instant.now().toEpochMilli();
        Random rand = new Random();
        long pad = rand.nextLong();
        String nonceString = Long.valueOf(time).toString()
                + Long.valueOf(pad).toString();
        byte mdbytes[] = messageDigest.digest(nonceString.getBytes());
        return toHexString(mdbytes);
    }

    /**
     * 给响应详细设置认证信息
     *
     * @param headerFactory SIP请求头工厂
     * @param response SIP响应
     * @param realm 认证域
     * @return SIP响应
     */
    public Response generateChallenge(HeaderFactory headerFactory, Response response, String realm) {
        try {
            WWWAuthenticateHeader proxyAuthenticate = headerFactory
                    .createWWWAuthenticateHeader(DEFAULT_SCHEME);
            proxyAuthenticate.setParameter("realm", realm);  // 认证域
            proxyAuthenticate.setParameter("qop", "auth");  // 保护质量
            proxyAuthenticate.setParameter("nonce", generateNonce());  // 一次性随机数
            proxyAuthenticate.setParameter("algorithm", DEFAULT_ALGORITHM); // 哈希算法

            response.setHeader(proxyAuthenticate);
        } catch (Exception ex) {
            InternalErrorHandler.handleException(ex);
        }
        return response;
    }
    /**
     * Authenticate the inbound request.
     *
     * @param request - the request to authenticate.
     * @param hashedPassword -- the MD5 hashed string of username:realm:plaintext password.
     *
     * @return true if authentication succeded and false otherwise.
     */
    public boolean doAuthenticateHashedPassword(Request request, String hashedPassword) {
        AuthorizationHeader authHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if ( authHeader == null ) {
            return false;
        }
        String realm = authHeader.getRealm();
        String username = authHeader.getUsername();

        if ( username == null || realm == null ) {
            return false;
        }

        String nonce = authHeader.getNonce();
        URI uri = authHeader.getURI();
        if (uri == null) {
            return false;
        }



        String A2 = request.getMethod().toUpperCase() + ":" + uri.toString();
        String HA1 = hashedPassword;


        byte[] mdbytes = messageDigest.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);

        String cnonce = authHeader.getCNonce();
        String KD = HA1 + ":" + nonce;
        if (cnonce != null) {
            KD += ":" + cnonce;
        }
        KD += ":" + HA2;
        mdbytes = messageDigest.digest(KD.getBytes());
        String mdString = toHexString(mdbytes);
        String response = authHeader.getResponse();


        return mdString.equals(response);
    }

    /**
     * Authenticate the inbound request given plain text password.
     *
     * @param request - the request to authenticate.
     * @param pass -- the plain text password.
     *
     * @return true if authentication succeded and false otherwise.
     */
    public boolean doAuthenticatePlainTextPassword(Request request, String pass) {
        AuthorizationHeader authHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if ( authHeader == null || authHeader.getRealm() == null) {
            return false;
        }
        log.debug("鉴权参数:{}", authHeader.toString());
        String realm = authHeader.getRealm().trim();
        String username = authHeader.getUsername().trim();

        if ( username == null || realm == null ) {
            return false;
        }

        String nonce = authHeader.getNonce();
        URI uri = authHeader.getURI();
        if (uri == null) {
            return false;
        }
        // qop 保护质量 包含auth（默认的）和auth-int（增加了报文完整性检测）两种策略
        String qop = authHeader.getQop();

        // 客户端随机数，这是一个不透明的字符串值，由客户端提供，并且客户端和服务器都会使用，以避免用明文文本。
        // 这使得双方都可以查验对方的身份，并对消息的完整性提供一些保护
        String cnonce = authHeader.getCNonce();

        // nonce计数器，是一个16进制的数值，表示同一nonce下客户端发送出请求的数量
        int nc = authHeader.getNonceCount();
        String ncStr = String.format("%08x", nc).toUpperCase();

        String A1 = username + ":" + realm + ":" + pass;

        String A2 = request.getMethod().toUpperCase() + ":" + uri.toString();

        byte[] mdbytes = messageDigest.digest(A1.getBytes());
        String HA1 = toHexString(mdbytes);
        log.debug("A1: " + A1);
        log.debug("A2: " + A2);
        mdbytes = messageDigest.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);
        log.debug("HA1: " + HA1);
        log.debug("HA2: " + HA2);

        String KD = HA1 + ":" + nonce;

        if (qop != null && qop.equalsIgnoreCase("auth") ) {
            if (nc != -1) {
                KD += ":" + ncStr;
            }
            if (cnonce != null) {
                KD += ":" + cnonce;
            }
            KD += ":" + qop;
        }
        KD += ":" + HA2;
        log.debug("KD: " + KD);
        mdbytes = messageDigest.digest(KD.getBytes());
        String mdString = toHexString(mdbytes);

        String response = authHeader.getResponse();
        log.debug("mdString:[" + mdString + "], response:[" + response + "]");
        return mdString.equals(response);

    }

}
