package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * TranslatorTest unit test.
 *
 * @author jantezana
 * @version 2017/02/03
 */
public class TranslatorTest {

    /*********************
     * convertToGroupDTO *
     *********************/

    @Test
    public void verifyThatGroupTypeContainsOnlyTheId()
    throws Exception {
        Group group = buildGroup();
        GroupDto groupDto = Translator.convertToGroupDTO(group);
        GroupTypeDto expected = new GroupTypeDto();
        expected.id = 2L;
        GroupTypeDto actual = groupDto.groupType;
        assertEquals(expected, actual);
    }

    @Test
    public void verifyThatGroupTypeNotContainsTheSameId()
    throws Exception {
        Group group = buildGroup();
        GroupDto groupDto = Translator.convertToGroupDTO(group);
        GroupTypeDto expected = new GroupTypeDto();
        expected.id = 3L;
        GroupTypeDto actual = groupDto.groupType;
        assertNotEquals(expected, actual);
    }

    @Test
    public void verifyThatGroupTypeNotContainsManyFields()
    throws Exception {
        Group group = buildGroup();
        GroupDto groupDto = Translator.convertToGroupDTO(group);
        GroupTypeDto expected = new GroupTypeDto();
        expected.id = 2L;
        expected.name = "GroupType0001";
        expected.code = "GroupType0001";
        GroupTypeDto actual = groupDto.groupType;
        assertNotEquals(expected, actual);
    }

    @Test
    public void verifyThatGroupTypeIsnull()
    throws Exception {
        Group group = buildAGroupWithoutGroupType();
        GroupDto groupDto = Translator.convertToGroupDTO(group);
        GroupTypeDto expected = null;
        GroupTypeDto actual = groupDto.groupType;
        assertEquals(expected, actual);
    }

    /**
     * Builds a group entity.
     *
     * @return the group
     */
    private Group buildGroup() {
        Group group = new Group();
        group.setId(1L);
        group.setName("Group0001");
        group.setCode("Group0001");

        GroupType groupType = new GroupType();
        groupType.setId(2L);
        groupType.setName("GroupType0001");
        groupType.setCode("GroupType0001");
        group.setGroupType(groupType);

        group.setHierarchyName(">Mns");
        group.setArchived(false);
        group.setTreeLevel(3);

        return group;
    }

    /**
     * Builds a group entity.
     *
     * @return the group
     */
    private Group buildAGroupWithoutGroupType() {
        Group group = new Group();
        group.setId(1L);
        group.setName("Group0001");
        group.setCode("Group0001");
        group.setHierarchyName(">Mns");
        group.setArchived(false);
        group.setTreeLevel(3);

        return group;
    }
}