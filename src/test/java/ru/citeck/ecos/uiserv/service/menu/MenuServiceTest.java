package ru.citeck.ecos.uiserv.service.menu;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.repository.MenuRepository;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = Application.class)
public class MenuServiceTest {
    @MockBean
    private MenuRepository menuRepository;

    private MenuService menuService;

//    @Before
//    public void setup() {
//        this.menuService = new MenuService(menuRepository);
//    }
//
//    @Test
//    public void getMenu_entityDoNotExist_emptyOptional() {
//        String menuId = "nonExistent";
//
//        when(menuRepository.findByExtId(menuId)).thenReturn(Optional.empty());
//
//        assertEquals(Optional.empty(), menuService.getMenu(menuId));
//    }

//    @Test
//    public void getMenu_entityExists_properDto() {
//        String menuId = "menuId";
//        String type = "type";
//        String authorities = "authorities";
//        String config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//            "<menu-config xmlns=\"http://www.citeck.ru/menu/config/1.0\">\n" +
//            "\n" +
//            "    <id>default-menu</id>\n" +
//            "    <type>LEFT_MENU</type>\n" +
//            "    <authorities>GROUP_EVERYONE</authorities>\n" +
//            "\n" +
//            "    <items>\n" +
//            "        <item id=\"HEADER_TASKS\">\n" +
//            "            <label>menu.header.tasks</label>\n" +
//            "            <items>\n" +
//            "                <resolver id=\"JOURNALS\">\n" +
//            "                    <param name=\"listId\">global-tasks</param>\n" +
//            "                    <param name=\"displayCount\">true</param>\n" +
//            "                    <param name=\"countForJournals\">active-tasks,subordinate-tasks</param>\n" +
//            "                    <item>\n" +
//            "                        <mobile-visible>false</mobile-visible>\n" +
//            "                        <items>\n" +
//            "                            <resolver id=\"JOURNAL_FILTERS\" />\n" +
//            "                        </items>\n" +
//            "                    </item>\n" +
//            "                </resolver>\n" +
//            "            </items>\n" +
//            "        </item>\n" +
//            "        <item id=\"HEADER_SITES\">\n" +
//            "            <label>menu.header.sites</label>\n" +
//            "            <items>\n" +
//            "                <resolver id=\"USER_SITES\">\n" +
//            "                    <item>\n" +
//            "                        <items>\n" +
//            "                            <resolver id=\"SITE_JOURNALS\">\n" +
//            "                                <item>\n" +
//            "                                    <mobile-visible>false</mobile-visible>\n" +
//            "                                    <items>\n" +
//            "                                        <resolver id=\"JOURNAL_FILTERS\" />\n" +
//            "                                    </items>\n" +
//            "                                </item>\n" +
//            "                            </resolver>\n" +
//            "                            <resolver id=\"SITE_DOCUMENT_LIBRARY\" />\n" +
//            "                            <resolver id=\"SITE_CALENDAR\" />\n" +
//            "                        </items>\n" +
//            "                    </item>\n" +
//            "                </resolver>\n" +
//            "                <item id=\"HEADER_SITES_SEARCH\">\n" +
//            "                    <label>menu.item.find-sites</label>\n" +
//            "                    <icon>find-sites</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">custom-site-finder</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_SITES_CREATE\">\n" +
//            "                    <label>menu.item.create-site</label>\n" +
//            "                    <icon>create-site</icon>\n" +
//            "                    <action type=\"CREATE_SITE\" />\n" +
//            "                </item>\n" +
//            "            </items>\n" +
//            "        </item>\n" +
//            "        <item id=\"HEADER_ORGSTRUCT\">\n" +
//            "            <label>menu.header.orgstructure</label>\n" +
//            "            <items>\n" +
//            "                <item id=\"HEADER_ORGSTRUCT_WIDGET\">\n" +
//            "                    <label>menu.header.orgstructure</label>\n" +
//            "                    <icon>orgstructure</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">orgstruct</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "            </items>\n" +
//            "        </item>\n" +
//            "        <item id=\"HEADER_MORE_MY_GROUP\">\n" +
//            "            <label>menu.header.my</label>\n" +
//            "            <items>\n" +
//            "                <item id=\"HEADER_MY_WORKFLOWS\">\n" +
//            "                    <label>menu.item.my-workflows</label>\n" +
//            "                    <icon>my-workflows</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">my-workflows</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_COMPLETED_WORKFLOWS\">\n" +
//            "                    <label>menu.item.completed-workflows</label>\n" +
//            "                    <icon>completed-workflows</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">completed-workflows</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_MY_CONTENT\">\n" +
//            "                    <label>menu.item.my-content</label>\n" +
//            "                    <icon>my-content</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">user/user-content</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_MY_SITES\">\n" +
//            "                    <label>menu.item.my-sites</label>\n" +
//            "                    <icon>my-sites</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">user/user-sites</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_MY_FILES\">\n" +
//            "                    <label>menu.item.my-files</label>\n" +
//            "                    <icon>my-files</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">context/mine/myfiles</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_DATA_LISTS\">\n" +
//            "                    <label>menu.item.data-lists</label>\n" +
//            "                    <icon>data-lists</icon>\n" +
//            "                    <param name=\"hideEmpty\">true</param>\n" +
//            "                    <items>\n" +
//            "                        <item id=\"HEADER_COMMON_DATA_LISTS\">\n" +
//            "                            <label>menu.item.common-data-lists</label>\n" +
//            "                            <icon>data-lists</icon>\n" +
//            "                            <action type=\"PAGE_LINK\">\n" +
//            "                                <param name=\"pageId\">journals2/list/main</param>\n" +
//            "                            </action>\n" +
//            "                            <items>\n" +
//            "                                <resolver id=\"JOURNALS\">\n" +
//            "                                    <param name=\"listId\">global-main</param>\n" +
//            "                                </resolver>\n" +
//            "                            </items>\n" +
//            "                        </item>\n" +
//            "                        <resolver id=\"USER_SITES_REFERENCES\">\n" +
//            "                            <item>\n" +
//            "                                <icon>data-lists</icon>\n" +
//            "                                <items>\n" +
//            "                                    <resolver id=\"SITE_JOURNALS\">\n" +
//            "                                        <param name=\"listId\">references</param>\n" +
//            "                                    </resolver>\n" +
//            "                                </items>\n" +
//            "                            </item>\n" +
//            "                        </resolver>\n" +
//            "                    </items>\n" +
//            "                </item>\n" +
//            "            </items>\n" +
//            "        </item>\n" +
//            "        <item id=\"HEADER_MORE_TOOLS_GROUP\">\n" +
//            "            <label>menu.header.admin-tools</label>\n" +
//            "            <evaluator id=\"user-in-group\">\n" +
//            "                <param name=\"groupName\">GROUP_ALFRESCO_ADMINISTRATORS</param>\n" +
//            "            </evaluator>\n" +
//            "            <items>\n" +
//            "                <item id=\"HEADER_REPOSITORY\">\n" +
//            "                    <label>menu.item.repository</label>\n" +
//            "                    <icon>repository</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">repository</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_APPLICATION\">\n" +
//            "                    <label>menu.item.application</label>\n" +
//            "                    <icon>application</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">console/admin-console/application</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_FLOWABLE_MODELER\">\n" +
//            "                    <label>menu.item.bpmn-modeler</label>\n" +
//            "                    <icon>bpmn-modeler</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">bpmn-designer</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_GROUPS\">\n" +
//            "                    <label>menu.item.groups</label>\n" +
//            "                    <icon>groups</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">console/admin-console/groups</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_USERS\">\n" +
//            "                    <label>menu.item.users</label>\n" +
//            "                    <icon>users</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">console/admin-console/users</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_TYPES\">\n" +
//            "                    <label>menu.item.types</label>\n" +
//            "                    <icon>types</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">console/admin-console/type-manager</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_SYSTEM_JOURNALS\">\n" +
//            "                    <label>menu.item.system-journals</label>\n" +
//            "                    <icon>system-journals</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">journals2/list/system</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_META_JOURNALS\">\n" +
//            "                    <label>menu.item.journal-setup</label>\n" +
//            "                    <icon>journal-setup</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">journals2/list/meta</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_TEMPLATES\">\n" +
//            "                    <label>menu.item.templates</label>\n" +
//            "                    <icon>templates</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">journals2/list/templates</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_DEV_TOOLS\">\n" +
//            "                    <label>menu.item.dev-tools</label>\n" +
//            "                    <icon>dev-tools</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">dev-tools</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "                <item id=\"HEADER_MORE\">\n" +
//            "                    <label>menu.item.more</label>\n" +
//            "                    <icon>more</icon>\n" +
//            "                    <action type=\"PAGE_LINK\">\n" +
//            "                        <param name=\"pageId\">console/admin-console/</param>\n" +
//            "                        <param name=\"section\">javascript-console</param>\n" +
//            "                    </action>\n" +
//            "                </item>\n" +
//            "            </items>\n" +
//            "        </item>\n" +
//            "    </items>\n" +
//            "</menu-config>\n";
//        Integer modelVersion = 1;
//        String localization = "{\"en\":{\"text\":\"text\"},\"ru\":{\"text\":\"текст\"}}";
//
//        MenuEntity entity = new MenuEntity();
//        entity.setId(1L);
//        entity.setExtId(menuId);
//        entity.setType(type);
//        entity.setAuthorities(authorities);
//        entity.setConfig(config);
//        entity.setModelVersion(modelVersion);
//        entity.setLocalization(localization);
//        when(menuConfigurationRepository.findByExtId(menuId)).thenReturn(Optional.of(entity));
//
//        Optional<MenuDto> gotOpt = menuConfigurationService.getMenu(menuId);
//        assertTrue(gotOpt.isPresent());
//
//        MenuDto got = gotOpt.get();
//        assertEquals(menuId, got.getId());
//        assertEquals(type, got.getType());
//        assertEquals(authorities, got.getAuthorities());
//        assertEquals(modelVersion, got.getModelVersion());
//
//        assertEquals("text", got.getLocalizedString("text", Locale.ENGLISH));
//        assertEquals("текст", got.getLocalizedString("text", new Locale("ru")));
//    }
}
