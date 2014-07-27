package com.continuuity.loom.provisioner;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.Entities;
import com.continuuity.loom.account.Account;
import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.scheduler.task.MissingEntityException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class TenantProvisionerServiceTest extends BaseTest {
  private static TenantProvisionerService service;
  private static MockProvisionerRequestService provisionerRequestService;

  @BeforeClass
  public static void setupTestClass() {
    service = injector.getInstance(TenantProvisionerService.class);
    provisionerRequestService = injector.getInstance(MockProvisionerRequestService.class);
  }

  @Before
  public void setupTest() {
    provisionerRequestService.reset();
  }

  @Test(expected = QuotaException.class)
  public void testExceptionWhenQuotaExceeded() throws Exception {
    // 2 cluster quota, 5 node quota
    Tenant tenant = new Tenant("tenantX", "id123", 50, 2, 5);
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    service.writeProvisioner(provisioner);
    service.writeTenant(tenant);

    // write a cluster with 2 nodes, quotas should be fine
    Account account = new Account("user1", tenant.getId());
    Cluster cluster = new Cluster(
      "104", account, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    clusterStoreService.getView(account).writeCluster(cluster);

    // quotas should be fine with 1 more cluster
    Assert.assertTrue(service.satisfiesTenantQuotas(tenant, 1, 0));
    // quotas should not be fine with 2 more clusters
    Assert.assertFalse(service.satisfiesTenantQuotas(tenant, 2, 0));
    // quotas should be fine with 3 more nodes
    Assert.assertTrue(service.satisfiesTenantQuotas(tenant, 0, 3));
    // quotas should not be fine with 4 more nodes
    Assert.assertFalse(service.satisfiesTenantQuotas(tenant, 0, 4));

    // lowering the quotas should throw the exception
    service.writeTenant(new Tenant(tenant.getName(), tenant.getId(), tenant.getWorkers(), 1, 1));
  }

  @Test
  public void testAddWorkersToProvisioner() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 50, 10, 100);
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    service.writeProvisioner(provisioner);
    service.writeTenant(tenant);

    service.rebalanceTenantWorkers(tenant.getId());
    Assert.assertEquals(tenant.getWorkers(),
                        service.getProvisioner(provisioner.getId()).getAssignedWorkers(tenant.getId()));
  }

  @Test
  public void testRemoveWorkersFromProvisioner() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 10, 10, 100);
    Provisioner provisioner =
      new Provisioner("p1", "host", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(tenant.getId(), tenant.getWorkers()));
    service.writeProvisioner(provisioner);
    service.writeTenant(tenant);

    service.rebalanceTenantWorkers(tenant.getId());
    Assert.assertEquals(tenant.getWorkers(),
                        service.getProvisioner(provisioner.getId()).getAssignedWorkers(tenant.getId()));
  }

  @Test
  public void testDeadProvisionerGetsDeletedDuringAddWorkers() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 110, 10, 100);
    Provisioner provisioner1 = new Provisioner("p1", "host1", 12345, 100, null, null);
    Provisioner provisioner2 = new Provisioner("p2", "host2", 12345, 100, null, null);
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    service.writeTenant(tenant);

    provisionerRequestService.addDeadProvisioner(provisioner1.getId());

    try {
      service.rebalanceTenantWorkers(tenant.getId());
    } catch (CapacityException e) {
      // expected
    }
    // provisioner should have been deleted
    Assert.assertNull(service.getProvisioner(provisioner1.getId()));
  }

  @Test
  public void testDeadProvisionerGetsDeletedDuringRemoveWorkers() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 40, 10, 100);
    Provisioner provisioner1 = new Provisioner("p1", "host1", 12345, 100, null,
                                               ImmutableMap.<String, Integer>of(tenant.getId(), 50));
    Provisioner provisioner2 = new Provisioner("p2", "host2", 12345, 100, null, null);
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    service.writeTenant(tenant);

    provisionerRequestService.addDeadProvisioner(provisioner1.getId());

    try {
      service.rebalanceTenantWorkers(tenant.getId());
    } catch (CapacityException e) {
      // expected
    }
    // provisioner1 should have been deleted
    Assert.assertNull(service.getProvisioner(provisioner1.getId()));

    // normally the tenant would have been queued for rebalancing.  Here we'll just call it directly.
    service.rebalanceTenantWorkers(tenant.getId());
    // workers should have been transferred to provisioner2
    Provisioner p2 = service.getProvisioner(provisioner2.getId());
    Assert.assertEquals(tenant.getWorkers(), p2.getAssignedWorkers(tenant.getId()));
  }

  @Test
  public void testDeleteTenant() throws Exception {
    Tenant tenant1 = new Tenant("tenant1", "tenant1", 0, 10, 100);
    Tenant tenant2 = new Tenant("tenant2", "tenant2", 10, 10, 100);
    Provisioner provisioner1 =
      new Provisioner("p1", "host1", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(
                        tenant2.getId(), 5));
    Provisioner provisioner2 =
      new Provisioner("p2", "host2", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(
                        tenant2.getId(), 5));
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    service.writeTenant(tenant1);
    service.writeTenant(tenant2);

    service.deleteTenant(tenant1.getId());
    Provisioner actualProvisioner1 = service.getProvisioner(provisioner1.getId());
    Provisioner actualProvisioner2 = service.getProvisioner(provisioner2.getId());
    Assert.assertTrue(!actualProvisioner1.getAssignedTenants().contains(tenant1.getId()));
    Assert.assertTrue(!actualProvisioner2.getAssignedTenants().contains(tenant1.getId()));
  }

  @Test(expected = IllegalStateException.class)
  public void testDeleteTenantWithAssignedWorkersThrowsException() throws Exception {
    Tenant tenant1 = new Tenant("tenant1", "tenant1", 10, 10, 100);
    Provisioner provisioner1 =
      new Provisioner("p1", "host1", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(
                        tenant1.getId(), 10));
    service.writeProvisioner(provisioner1);
    service.writeTenant(tenant1);
    service.deleteTenant(tenant1.getId());
  }

  @Test
  public void testHeartbeatChanges() throws Exception {
    Tenant tenant = new Tenant("tenant1", "tenant1", 10, 10, 100);
    Provisioner provisioner =
      new Provisioner("p1", "host1", 12345, 100,
                      ImmutableMap.<String, Integer>of(tenant.getId(), 5),
                      ImmutableMap.<String, Integer>of(tenant.getId(), 10));
    service.writeProvisioner(provisioner);
    service.writeTenant(tenant);

    ProvisionerHeartbeat heartbeat1 = new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of(tenant.getId(), 5));
    ProvisionerHeartbeat heartbeat2 = new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of(tenant.getId(), 10));
    ProvisionerHeartbeat heartbeat3 = new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of());

    service.handleHeartbeat(provisioner.getId(), heartbeat1);
    Provisioner actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(5, actualProvisioner.getLiveWorkers(tenant.getId()));

    service.handleHeartbeat(provisioner.getId(), heartbeat2);
    actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(10, actualProvisioner.getLiveWorkers(tenant.getId()));

    service.handleHeartbeat(provisioner.getId(), heartbeat3);
    actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(0, actualProvisioner.getLiveWorkers(tenant.getId()));
    Assert.assertTrue(actualProvisioner.getLiveTenants().isEmpty());
  }

  @Test
  public void testHeartbeatAfterDeleteTenant() throws Exception {
    Tenant tenant = new Tenant("tenant1", "tenant1", 0, 10, 100);
    Provisioner provisioner =
      new Provisioner("p1", "host1", 12345, 100, ImmutableMap.<String, Integer>of(tenant.getId(), 5), null);
    service.writeProvisioner(provisioner);
    service.writeTenant(tenant);

    service.deleteTenant(tenant.getId());
    service.handleHeartbeat(provisioner.getId(), new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of()));
    Provisioner actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertTrue(actualProvisioner.getAssignedTenants().isEmpty());
    Assert.assertTrue(actualProvisioner.getLiveTenants().isEmpty());
  }

  @Test(expected = MissingEntityException.class)
  public void testHeartbeatForNonexistantProvisionerThrowsException() throws Exception {
    service.handleHeartbeat("id123", new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of()));
  }
}
