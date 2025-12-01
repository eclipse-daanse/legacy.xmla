package mondrian.test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.CubeGrandDataLoader;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandCubeGrandCatalog;
import org.opencube.junit5.propupdator.AppandCatalogGrandCatalog;
import org.opencube.junit5.propupdator.AppandDimensionGrandCatalog;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.opencube.junit5.propupdator.AppandHierarchyGrandCatalog;
import org.opencube.junit5.propupdator.AppandMemberGrandCatalog;

import org.opencube.junit5.propupdator.AppandDefaultRoleCatalog;



public class AccessTest {

    //CubeGrand
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCubeGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testCubeGrand(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "42");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCubeGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testCubeGrandNoAccessCube2(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());

        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube2] WHERE ([Measures].[Measure1])",
                "MDX cube 'Cube2' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCubeGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testCubeGrandNoAccessRole2(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());

        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube2] WHERE ([Measures].[Measure1])",
                "Internal error: Role 'role2' not found");
    }

    //CatalogGrand
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCatalogGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testCatalogGrandRoleAll(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("roleAll"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "42");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCatalogGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testCatalogGrandRoleAllDimWithCubeGrand(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("roleAllDimWithCubeGrand"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "42");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCatalogGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testCatalogGrandRoleAllDimWithoutCubeGrand(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("roleAllDimWithoutCubeGrand"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
                "MDX cube 'Cube1' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCatalogGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testCatalogGrandRoleNone(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("roleNone"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
                "MDX cube 'Cube1' not found");
    }

    //DefaultRole
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDefaultRoleCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDefaultRole(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "84");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDefaultRoleCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDefaultRoleNoRole(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of(), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "84");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDefaultRoleCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDefaultRoleRole2Absent(Context<?> context) {
        //catalog have role1. but role2 is absent
        ConnectionProps props =new ConnectionProps(List.of("role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
                "Internal error: Role 'role2' not found");
    }

    //DimensionGrand
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDimensionGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDimensionGrand(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "84");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDefaultRoleCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDimensionGrandRole2Absent(Context<?> context) {
        //catalog have role1. but role2 is absent
        ConnectionProps props =new ConnectionProps(List.of("role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
                "Internal error: Role 'role2' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDimensionGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDimensionGrandDimension1Hierarchy1(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[Dimension1].[Hierarchy1].[All Hierarchy1s]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "Axis #1:\n"
            + "{[Dimension1].[Hierarchy1].[All Hierarchy1s]}\n"
            + "{[Dimension1].[Hierarchy1].[A]}\n"
            + "{[Dimension1].[Hierarchy1].[B]}\n"
            + "{[Dimension1].[Hierarchy1].[C]}\n"
            + "Row #0: 84\n"
            + "Row #0: 42\n"
            + "Row #0: 21\n"
            + "Row #0: 21\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDimensionGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDimensionGrandDimension1Hierarchy2(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[Dimension1].[Hierarchy2].[All Hierarchy2s]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [Cube1] WHERE ([Measures].[Measure1])",
                "MDX object '[Dimension1].[Hierarchy2].[All Hierarchy2s]' not found in cube 'Cube1'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandDimensionGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testDimensionGrandDimension2(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[Dimension2].[Hierarchy1].[All Hierarchy1s]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [Cube1] WHERE ([Measures].[Measure1])",
                "MDX object '[Dimension2].[Hierarchy1].[All Hierarchy1s]' not found in cube 'Cube1'");
    }

    //HierarchyGrand
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandHierarchyGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testHierarchyGrand(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "84");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandHierarchyGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testHierarchyGrandRole2Absent(Context<?> context) {
        //catalog have role1. but role2 is absent
        ConnectionProps props =new ConnectionProps(List.of("role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
                "Internal error: Role 'role2' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandHierarchyGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testHierarchyGrandDimension1Hierarchy1(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[Dimension1].[Hierarchy1].[All Hierarchy1s]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "Axis #1:\n"
            + "{[Dimension1].[Hierarchy1].[All Hierarchy1s]}\n"
            + "{[Dimension1].[Hierarchy1].[A]}\n"
            + "{[Dimension1].[Hierarchy1].[B]}\n"
            + "{[Dimension1].[Hierarchy1].[C]}\n"
            + "Row #0: 84\n"
            + "Row #0: 42\n"
            + "Row #0: 21\n"
            + "Row #0: 21\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandHierarchyGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testHierarchyGrandDimension1Hierarchy2(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[Dimension1].[Hierarchy2].[All Hierarchy2s]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [Cube1] WHERE ([Measures].[Measure1])",
                "MDX object '[Dimension1].[Hierarchy2].[All Hierarchy2s]' not found in cube 'Cube1'");
    }

    //MemberGrand
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandMemberGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testMemberGrand(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "84");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandMemberGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testMemberGrandRole2Absent(Context<?> context) {
        //catalog have role1. but role2 is absent
        ConnectionProps props =new ConnectionProps(List.of("role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT FROM [Cube1] WHERE ([Measures].[Measure1])",
                "Internal error: Role 'role2' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandMemberGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testMemberGrandDimension1Hierarchy1(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[Dimension1].[Hierarchy1].[All Hierarchy1s]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [Cube1] WHERE ([Measures].[Measure1])",
            "Axis #0:\n"
            + "{[Measures].[Measure1]}\n"
            + "Axis #1:\n"
            + "{[Dimension1].[Hierarchy1].[All Hierarchy1s]}\n"
            + "{[Dimension1].[Hierarchy1].[A]}\n"
            + "Row #0: 84\n"
            + "Row #0: 42\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandMemberGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testMemberGrandDimension1Hierarchy2(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT NON EMPTY Hierarchize(AddCalculatedMembers({DrilldownLevel({[Dimension1].[Hierarchy2].[All Hierarchy2s]})})) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [Cube1] WHERE ([Measures].[Measure1])",
                "MDX object '[Dimension1].[Hierarchy2].[All Hierarchy2s]' not found in cube 'Cube1'");
    }
    
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testFoodMartAdministratorSales(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("Administrator"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT {[Measures].Members} ON COLUMNS FROM [SALES]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 86,837\n"
            + "Row #0: 5,581\n"
            + "Row #0: 151,211.21\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testFoodMartAdministratorHR(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("Administrator"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT {[Measures].Members} ON COLUMNS FROM [HR]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "{[Measures].[Count]}\n"
            + "{[Measures].[Number of Employees]}\n"
            + "Row #0: $39,431.67\n"
            + "Row #0: 7,392\n"
            + "Row #0: 616\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testFoodMartCaliforniaManageSales(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("California manager"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT {[Measures].Members} ON COLUMNS FROM [SALES]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 86,837\n"
            + "Row #0: 5,581\n"
            + "Row #0: 151,211.21\n");
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testFoodMartNoHRCubeSales(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("No HR Cube"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        TestUtil.assertQueryReturns(
            connection,
            "SELECT {[Measures].Members} ON COLUMNS FROM [SALES]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 86,837\n"
            + "Row #0: 5,581\n"
            + "Row #0: 151,211.21\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandCatalogGrandCatalog.class, dataloader = CubeGrandDataLoader.class )
    void testFoodMartNoHRCubeHR(Context<?> context) {
        ConnectionProps props =new ConnectionProps(List.of("roleAllDimWithoutCubeGrand"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
                context,
                props,
                "SELECT {[Measures].Members} ON COLUMNS FROM [HR]",
                "MDX cube 'HR' not found");
    }

}
