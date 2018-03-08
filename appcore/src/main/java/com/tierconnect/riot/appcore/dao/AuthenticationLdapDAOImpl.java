package com.tierconnect.riot.appcore.dao;

import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.InvalidNameException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.PartialResultException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import javax.naming.directory.*;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 8/30/16 11:05 AM
 * @version:
 */
public class AuthenticationLdapDAOImpl implements AuthenticationLdapDAO {

    private LdapTemplate ldapTemplate;
    String userDnConnection = null;
    private static final Logger logger = Logger.getLogger( AuthenticationLdapDAO.class );

    public void setLdapTemplate(LdapTemplate ldapTemplate){
        this.ldapTemplate=ldapTemplate;
    }

    public String getUserDnConnection() {
        return userDnConnection;
    }

    public void setUserDnConnection(String userDnConnection) {
        this.userDnConnection = userDnConnection;
    }

    /**
     * user authentication
     * @param userDn
     * @param credentials
     * @return
     */
    public boolean authenticate(String userDn,String credentials,String userIdentifier) {
        logger.info("For User "+userDn);
        LdapName base=null;
        try {
            logger.debug("ldap context *** "+ldapTemplate.toString());
            base = ((LdapContextSource) (( this).ldapTemplate).getContextSource()).getBaseLdapName();
            validateConnectionUser(base,userIdentifier);
            if (userIdentifier != null && !userIdentifier.isEmpty()){
                ldapTemplate.authenticate(query().where(userIdentifier).is(userDn), credentials);
            } else if(userDn.contains("@")){
                ldapTemplate.authenticate(query().where("userPrincipalName").is(userDn), credentials);
            }else{
                if (userDn.contains("\\")){
                    userDn = parseUserDn(userDn,base,userIdentifier);
                }
                ldapTemplate.authenticate(query().where("sAMAccountName").is(userDn), credentials);
            }
            logger.info(userDn + " has been authenticated using LDAP");
            return  true;
        }catch (CommunicationException e){
            logger.error("LDAP/AD service unavailable, please try again later.",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (AuthenticationException e){
            logger.error("["+userDn+"] Username and/or password incorrect.",e);
            return false;
        } catch (NameNotFoundException e){
            logger.error("Group name in [" + String.valueOf(base) + "] is not correct, please check your group configuration. ", e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (InvalidNameException e){
            logger.error("LDAP/AD [" + String.valueOf(base) + "] name is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (PartialResultException e){
            logger.error("LDAP/AD [" + String.valueOf(base)+ "] name and referral values are incorrect",e);
            return false;
        }catch (DataAccessException e){
            logger.error("["+userDn+"] Username and/or password incorrect.",e);
            return false;
        }catch (IllegalArgumentException e){
            logger.error("LDAP/AD referral value is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (NamingException e){
            logger.error("LDAP/AD [" + String.valueOf(base) + "] name is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }
    }

    /**
     * replace and add properties in the spring context
     * @param contextProperties
     */
    public void setContext(Map<String,String> contextProperties) {
        String referral;

        LdapContextSource ctxSrc = new LdapContextSource();
        try {
            // set base
            if (contextProperties.get("base") != null && !contextProperties.get("base").trim().isEmpty()) {
                ctxSrc.setBase(contextProperties.get("base"));
            } else {
                logger.error("LDAP/AD base value is null or invalid ");
                throw  new UserException("LDAP/AD service unavailable, please try again later.");
            }
            // set url
            if (contextProperties.get("url") != null && !contextProperties.get("url").trim().isEmpty()) {
                ctxSrc.setUrl(contextProperties.get("url"));
            } else {
                logger.error("LDAP/AD [" + contextProperties.get("base") + "] url value is null or invalid ");
                throw  new UserException("LDAP/AD service unavailable, please try again later.");
            }
            // set userDn
            if (contextProperties.get("userDn") != null && !contextProperties.get("userDn").trim().isEmpty()) {
                setUserDnConnection(contextProperties.get("userDn"));
                LdapName ldapName = ctxSrc.getBaseLdapName();
                String typeAuthentication = contextProperties.get("typeAuthentication");
                String userDn = parseUserDn(contextProperties.get("userDn"),ldapName, typeAuthentication);
                ctxSrc.setUserDn(userDn);
            }else{
                logger.error("LDAP/AD userDn value is null or invalid, check your LDAP configuration. ");
                throw  new UserException("LDAP/AD service unavailable, please try again later.");
            }
            // set password
            if (contextProperties.get("password") != null && !contextProperties.get("password").trim().isEmpty()) {
                ctxSrc.setPassword(contextProperties.get("password"));
            }else{
                logger.error("LDAP/AD [" + contextProperties.get("base") + "] password value is null or invalid ");
                throw  new UserException("LDAP/AD service unavailable, please try again later.");
            }
            //Referral default value is follow to avoid PartialResultException
            if(contextProperties.get("referral") == null || contextProperties.get("referral").length() == 0){
                ctxSrc.setReferral("follow");
                referral="follow";
            }else{
                ctxSrc.setReferral(contextProperties.get("referral"));
                referral=contextProperties.get("referral");
            }
            if(contextProperties.containsKey("ldapTimeOut")){
                Long time = Long.parseLong(contextProperties.get("ldapTimeOut"))*1000;
                Map<String,Object> baseProps = new HashMap<>();
                baseProps.put("com.sun.jndi.ldap.connect.timeout",time);
                ctxSrc.setBaseEnvironmentProperties(baseProps);
            }
        }catch (InvalidNameException e){
            logger.error("LDAP/AD [" + contextProperties.get("base") + "] name is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }
        logger.info("LDAP Connection Url: "+contextProperties.get("url")+" baseDn: "+ctxSrc.getBaseLdapPathAsString()+" userDn: "+ctxSrc.getUserDn()+" Referral: "+ referral);
        ctxSrc.afterPropertiesSet(); // this method should be called.
        ldapTemplate.setContextSource(ctxSrc);
    }

    /**
     * search user
     * @param userDn
     *
     */
    public boolean userExists(String userDn,String userIdentifier){
        LdapName base;
        boolean result=false;
        base = ((LdapContextSource) (( this).ldapTemplate).getContextSource()).getBaseLdapName();
        validateConnectionUser(base,userIdentifier);
        try {
            if (userIdentifier == null || userIdentifier.isEmpty()){
                if (userDn.contains("@")){
                    userIdentifier = "userPrincipalName";
                } else {
                    if (userDn.contains("\\")){
                        userDn = parseUserDn(userDn,base, userIdentifier);
                    }
                    userIdentifier = "sAMAccountName";
                }
            }
            logger.info("Values for ldap search: userIdentifier = " + userIdentifier + ", userDn = " + userDn);
            List cnList= ldapTemplate.search(query().where(userIdentifier).is(userDn),
                    new AttributesMapper() {
                        public Object mapFromAttributes(Attributes attrs)
                                throws NamingException, javax.naming.NamingException {
                            return attrs.get("cn").get();
                        }
                    });

            if(!cnList.isEmpty()){
                result=true;
            }
        }catch (CommunicationException e){
            logger.error("LDAP/AD service unavailable, please try again later.",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (AuthenticationException e){
            logger.error("LDAP/AD username value is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        } catch (NameNotFoundException e){
            logger.error("Group name in [" + String.valueOf(base) + "] is not correct, please check your group configuration. ", e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (InvalidNameException e){
            logger.error("LDAP/AD [" + String.valueOf(base) + "] name is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (PartialResultException e){
            logger.error("LDAP/AD [" + String.valueOf(base)+ "] name and referral values are incorrect",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (DataAccessException e){
            logger.error("["+userDn+"] Username and/or password incorrect.",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (IllegalArgumentException e){
            logger.error("LDAP/AD referral value is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (NamingException e){
            logger.error("LDAP/AD [" + String.valueOf(base) + "] name is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (Exception e){
            logger.error("Error searching user [" + userDn + "] in LDAP/AD [" + String.valueOf(base) + "]",e);
        }
        return  result;
    }

    /**
     * encode password
     * @param user
     * @return
     */
    private String parseUserDn(String user,LdapName base, String typeAuthentication) {
        String parsedValue;
        String baseValue=null;
        if (base.isEmpty()){
            logger.error("BaseDn is not configured correctly");
            throw new UserException("BaseDn is not configured correctly");
        }

        if(user.contains("@")){
            return user;
        }else{
            int backslash;
            if(user.contains("\\")){
                if (typeAuthentication != null && (typeAuthentication.isEmpty() || typeAuthentication.equals("sAMAccountName"))){
                    String userTemporal = user.replace("\\","\\\\");
                    String[] userName = userTemporal.split("\\\\");
                    if (userName.length == 3 && base.getRdns().toString().contains(userName[0])){
                        parsedValue = userName[2];
                    }else {
                        parsedValue = user;
                    }
                }else {
                    backslash = user.indexOf("\\");
                    baseValue = "@" + user.substring(0, backslash) + "." + base.getRdn(0).getValue();
                    parsedValue = user.substring(backslash + 1, user.length()) + baseValue;
                }
                return parsedValue;
            }else{
                if (user.charAt(2)=='=' || user.charAt(3)=='='){
                    return user;
                }else {
                    if (base.size() == 1) {
                        baseValue = "@" + base.getRdn(0).getValue();
                    } else {
                        List<Rdn> listRdns = base.getRdns();
                        baseValue = "@";
                        for (int i = listRdns.size() - 1; i >= 0; i--) {
                            if (listRdns.get(i).getType().equals("dc")) {
                                if (i == 0) {
                                    baseValue = baseValue + listRdns.get(i).getValue();
                                } else {
                                    baseValue = baseValue + listRdns.get(i).getValue() + ".";
                                }
                            }
                        }
                    }
                    return user + baseValue;
                }
            }
        }
    }

    /**
     * validate if user defined in connection exists in LDAP server
     */
    private void validateConnectionUser(LdapName base,String userIdentifier){
        try{
            String password = ((LdapContextSource) (( this).ldapTemplate).getContextSource()).getPassword();
            if (userIdentifier != null && !userIdentifier.isEmpty()){
                try {
                    logger.info("Values for ldap authenticate: userIdentifier = " + userIdentifier + ", getUserDnConnection() = " + getUserDnConnection());
                    ldapTemplate.authenticate(query().where(userIdentifier).is(getUserDnConnection()), password);
                } catch (Exception e){
                    try {
                        logger.info("Values for ldap authenticate: userIdentifier is userPrincipalName, getUserDnConnection() = " + getUserDnConnection());
                        ldapTemplate.authenticate(query().where("userPrincipalName").is(getUserDnConnection()), password);
                    } catch (Exception ee){
                        logger.info("Values for ldap authenticate: userIdentifier is sAMAccountName, getUserDnConnection() = " + getUserDnConnection());
                        ldapTemplate.authenticate(query().where("sAMAccountName").is(getUserDnConnection()), password);
                    }
                }
            } else if(getUserDnConnection().contains("@")){
                ldapTemplate.authenticate(query().where("userPrincipalName").is(getUserDnConnection()), password);
            }else{
                ldapTemplate.authenticate(query().where("sAMAccountName").is(getUserDnConnection()), password);
            }
        }catch (CommunicationException e){
            logger.error("LDAP/AD service unavailable, please try again later.",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (AuthenticationException e){
            logger.error("["+getUserDnConnection()+"] Username and/or password incorrect.",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        } catch (NameNotFoundException e){
            logger.error("Group name in [" + String.valueOf(base) + "] is not correct, please check your group configuration. ", e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (InvalidNameException e){
            logger.error("LDAP/AD [" + String.valueOf(base) + "] name is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (PartialResultException e){
            logger.error("LDAP/AD [" + String.valueOf(base)+ "] name and referral values are incorrect",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (DataAccessException e){
            logger.error("["+getUserDnConnection()+"] Username and/or password incorrect.",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (IllegalArgumentException e) {
            logger.error("LDAP/AD referral value is invalid, check your LDAP configuration. ", e);
            throw new UserException("LDAP/AD service unavailable, please try again later.");
        }catch (NamingException e){
            logger.error("LDAP/AD [" + String.valueOf(base) + "] name is invalid, check your LDAP configuration. ",e);
            throw  new UserException("LDAP/AD service unavailable, please try again later.");
        }
    }

    public boolean testConnection (String username, String password){
        try {
            authenticate(username,password,null);
            return true;
        } catch (org.springframework.ldap.NamingException ne) {
            throw  new UserException(ne);
        }catch (Exception e){
            throw  new UserException(e);
        }
    }
}
