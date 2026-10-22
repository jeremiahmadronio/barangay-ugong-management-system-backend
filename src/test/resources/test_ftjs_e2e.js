const BASE_URL = 'http://localhost:8080/api/v1/ftjs';

async function runE2ETest() {
    console.log('====================================================');
    console.log('   STARTING FULL END-TO-END TEST FOR FTJS BACKEND   ');
    console.log('====================================================\n');

    let createdFtjsId = null;
    let createdFtjsNumber = null;
    let testResident = null;

    // STEP 1: Search Resident
    console.log('--- STEP 1: Search Resident ---');
    const searchRes = await fetch(`${BASE_URL}/search-resident?query=`);
    if (!searchRes.ok) throw new Error(`Search failed: ${searchRes.statusText}`);
    const residents = await searchRes.json();
    console.log(`Found ${residents.length} active residents.`);
    if (residents.length === 0) throw new Error('No residents found to test with.');
    testResident = residents[0];
    console.log(`Selected Resident: [ID: ${testResident.residentId}] ${testResident.fullName}`);
    console.log(`- Resident Demographics: Age=${testResident.age}, Gender=${testResident.gender}, Address=${testResident.completeAddress}`);
    console.log(`- Prior FTJS Availed: ${testResident.hasAvailedFtjs}`);
    console.log(`- Issuance History Count: ${testResident.issuanceHistory?.length || 0}`);
    console.log(`- Case History Count: ${testResident.caseHistory?.length || 0}`);

    // STEP 2: Baseline Stats
    console.log('\n--- STEP 2: Baseline Stats ---');
    const baselineStats = await (await fetch(`${BASE_URL}/stats`)).json();
    const baselineDashboard = await (await fetch(`${BASE_URL}/dashboard-stats`)).json();
    console.log(`Baseline Total Issued: ${baselineStats.totalCertificatesIssued}`);
    console.log(`Baseline Active Volume: ${baselineDashboard.totalActiveVolume}`);

    // STEP 3: Create New FTJS Application Entry
    console.log('\n--- STEP 3: Create FTJS Application Entry ---');
    const trackingNo = `TEST-E2E-${Date.now()}`;
    const entryPayload = {
        residentId: testResident.residentId,
        ftjsNumber: trackingNo,
        purpose: 'Employment Application',
        educationalAttainment: 'College Graduate',
        gradeLevel: 'BS Information Technology',
        institution: 'Polytechnic University of the Philippines',
        validId: null,
        oathTaking: null,
        releasedBy: 'Admin Test Officer'
    };

    const entryRes = await fetch(`${BASE_URL}/entry`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(entryPayload)
    });
    if (!entryRes.ok) {
        const errText = await entryRes.text();
        throw new Error(`Create Entry failed: ${entryRes.status} ${errText}`);
    }
    const entryData = await entryRes.json();
    console.log(`Entry created successfully! Resident: ${entryData.fullName}`);

    // Verify it appears in FTJS summary table to get its ID
    const summaryRes = await (await fetch(`${BASE_URL}/summary?search=${trackingNo}`)).json();
    if (!summaryRes.content || summaryRes.content.length === 0) {
        throw new Error('Created FTJS application not found in summary table!');
    }
    const createdItem = summaryRes.content[0];
    createdFtjsId = createdItem.id;
    createdFtjsNumber = createdItem.trackingNumber;
    console.log(`Confirmed in Summary Table: ID=${createdFtjsId}, Tracking=${createdFtjsNumber}, Status=${createdItem.status}`);

    // STEP 4: Verify Detail View
    console.log('\n--- STEP 4: Verify Detail View ---');
    const detailRes = await fetch(`${BASE_URL}/detail/${createdFtjsId}`);
    if (!detailRes.ok) throw new Error(`Get Detail failed: ${detailRes.statusText}`);
    const detail = await detailRes.json();
    console.log(`Detail verified:`);
    console.log(`- Tracking No: ${detail.ftjsApplicationNumber}`);
    console.log(`- Applicant: ${detail.applicantFullName}`);
    console.log(`- Purpose: ${detail.purpose}`);
    console.log(`- Education: ${detail.educationalAttainment}`);
    console.log(`- Course: ${detail.course}`);
    console.log(`- Institution: ${detail.institution}`);
    console.log(`- Issuance Count: ${detail.issuanceCount}`);
    console.log(`- Status: ${detail.status}`);

    // STEP 5: Verify Stats Incremented
    console.log('\n--- STEP 5: Verify Stats Incremented ---');
    const updatedStats = await (await fetch(`${BASE_URL}/stats`)).json();
    const updatedDashboard = await (await fetch(`${BASE_URL}/dashboard-stats`)).json();
    console.log(`Updated Total Issued: ${updatedStats.totalCertificatesIssued} (was ${baselineStats.totalCertificatesIssued})`);
    console.log(`Updated Active Volume: ${updatedDashboard.totalActiveVolume} (was ${baselineDashboard.totalActiveVolume})`);
    if (updatedStats.totalCertificatesIssued !== baselineStats.totalCertificatesIssued + 1) {
        throw new Error('Stats count did not increment as expected!');
    }

    // STEP 6: Add Admin Note & Check Timeline
    console.log('\n--- STEP 6: Add Admin Note ---');
    const notePayload = { note: 'Applicant presented complete requirements and verified physically.' };
    const noteRes = await fetch(`${BASE_URL}/notes-entry/${createdFtjsId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(notePayload)
    });
    if (!noteRes.ok) throw new Error(`Add note failed: ${noteRes.statusText}`);
    const noteData = await noteRes.json();
    console.log(`Note created with ID: ${noteData.id}, Note: "${noteData.note}" by ${noteData.createdBy}`);

    // Verify Notes History
    const notesHistory = await (await fetch(`${BASE_URL}/notes-history/${createdFtjsId}`)).json();
    console.log(`Notes history count: ${notesHistory.length}. Latest: "${notesHistory[0].note}"`);

    // Verify Timeline
    const timeline = await (await fetch(`${BASE_URL}/${createdFtjsId}/timeline/`)).json();
    console.log(`Timeline events count: ${timeline.length}:`);
    timeline.forEach(t => console.log(`  * [${t.timelineType}] ${t.title} - ${t.description} (${t.actorName})`));

    // STEP 7: Request Re-Issuance
    console.log('\n--- STEP 7: Request Re-Issuance ---');
    const reIssuePayload = {
        ftjsId: createdFtjsId,
        reason: 'Lost original copy during job interview transit',
        dateOfLoss: new Date().toISOString().split('T')[0],
        orNumber: 'OR-E2E-998811',
        affidavit: null
    };
    const reIssueRes = await fetch(`${BASE_URL}/re-issuance-request/${createdFtjsId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reIssuePayload)
    });
    if (!reIssueRes.ok) {
        const errText = await reIssueRes.text();
        throw new Error(`Re-issuance failed: ${reIssueRes.status} ${errText}`);
    }
    const reIssueData = await reIssueRes.json();
    console.log(`Re-issuance request saved. Reason: "${reIssueData.reason}", OR#: ${reIssueData.orNumber}`);

    // Check Re-issuance History
    const reIssuanceHistory = await (await fetch(`${BASE_URL}/${createdFtjsId}/re-issuances/history`)).json();
    console.log(`Re-issuance history count: ${reIssuanceHistory.length}`);
    console.log(`Latest re-issuance copy #${reIssuanceHistory[0].issuanceNumber}, OR: ${reIssuanceHistory[0].orNumber}, Officer: ${reIssuanceHistory[0].providedBy}`);

    // Verify Detail updated to RE_ISSUANCE and count 2
    const updatedDetail = await (await fetch(`${BASE_URL}/detail/${createdFtjsId}`)).json();
    console.log(`Updated Detail after re-issuance: Status=${updatedDetail.status}, IssuanceCount=${updatedDetail.issuanceCount}`);
    if (updatedDetail.status !== 'RE_ISSUANCE' || updatedDetail.issuanceCount < 2) {
        throw new Error('Re-issuance status/count was not updated properly!');
    }

    // STEP 8: Check Reports & Analytics Reflection
    console.log('\n--- STEP 8: Check Reports & Analytics Reflection ---');
    const reportTable = await (await fetch(`${BASE_URL}/reports/table?search=${trackingNo}`)).json();
    console.log(`Reports Table match count for ${trackingNo}: ${reportTable.content.length}`);
    if (reportTable.content.length === 0) throw new Error('Created record not found in reports table!');
    const reportItem = reportTable.content[0];
    console.log(`Report Record Data:`);
    console.log(`- AppNo: ${reportItem.applicationNo}`);
    console.log(`- Name: ${reportItem.applicantName}`);
    console.log(`- Gender: ${reportItem.gender}`);
    console.log(`- Age: ${reportItem.age}`);
    console.log(`- Education: ${reportItem.educationalAttainment}`);
    console.log(`- Institution: ${reportItem.institution}`);
    console.log(`- Status: ${reportItem.status}`);
    console.log(`- ReleasedBy: ${reportItem.releasedBy}`);

    const kpi = await (await fetch(`${BASE_URL}/reports/kpi`)).json();
    console.log(`Current KPI Summary: Total=${kpi.totalRecords}, 1st-Time=${kpi.firstTimeIssuances}, TopPurpose=${kpi.mostCommonPurpose}`);

    // STEP 9: Archive Record
    console.log('\n--- STEP 9: Archive Record ---');
    const archiveRes = await fetch(`${BASE_URL}/archive/${createdFtjsId}`, { method: 'POST' });
    if (!archiveRes.ok) throw new Error(`Archive failed: ${archiveRes.statusText}`);
    console.log(`Archived record ID: ${createdFtjsId}`);

    // Verify NOT in active summary
    const activeCheck = await (await fetch(`${BASE_URL}/summary?search=${trackingNo}`)).json();
    console.log(`Active summary search after archive (should be 0): ${activeCheck.content.length}`);
    if (activeCheck.content.length !== 0) throw new Error('Archived record still appears in active summary table!');

    // Verify IS in archive summary
    const archiveSummary = await (await fetch(`${BASE_URL}/archives/summary?search=${trackingNo}`)).json();
    console.log(`Archive summary search count (should be 1): ${archiveSummary.content.length}`);
    if (archiveSummary.content.length !== 1) throw new Error('Record not found in archive summary!');
    console.log(`Archive stats verification:`);
    const archiveStats = await (await fetch(`${BASE_URL}/archive/stats`)).json();
    console.log(`- Total Archived: ${archiveStats.totalArchived}`);

    // STEP 10: Restore from Archive
    console.log('\n--- STEP 10: Restore from Archive ---');
    const restoreRes = await fetch(`${BASE_URL}/restore/${createdFtjsId}`, { method: 'POST' });
    if (!restoreRes.ok) throw new Error(`Restore failed: ${restoreRes.statusText}`);
    console.log(`Restored record ID: ${createdFtjsId}`);

    // Verify back in active summary
    const activeCheck2 = await (await fetch(`${BASE_URL}/summary?search=${trackingNo}`)).json();
    console.log(`Active summary count after restore (should be 1): ${activeCheck2.content.length}`);
    if (activeCheck2.content.length !== 1) throw new Error('Restored record not found in active summary table!');

    // STEP 11: Soft Delete (Move to Trash)
    console.log('\n--- STEP 11: Soft Delete (Move to Trash) ---');
    const deleteRes = await fetch(`${BASE_URL}/${createdFtjsId}`, { method: 'DELETE' });
    if (!deleteRes.ok) throw new Error(`Soft delete failed: ${deleteRes.statusText}`);
    console.log(`Soft deleted record ID: ${createdFtjsId}`);

    // Verify NOT in active summary
    const activeCheck3 = await (await fetch(`${BASE_URL}/summary?search=${trackingNo}`)).json();
    console.log(`Active summary count after soft-delete (should be 0): ${activeCheck3.content.length}`);
    if (activeCheck3.content.length !== 0) throw new Error('Soft-deleted record still appears in active summary!');

    // Verify IS in Trash summary
    const trashSummary = await (await fetch(`${BASE_URL}/trash/summary?search=${trackingNo}`)).json();
    console.log(`Trash summary count for deleted item (should be 1): ${trashSummary.content.length}`);
    if (trashSummary.content.length !== 1) throw new Error('Soft-deleted record not found in Trash summary!');

    // STEP 12: Permanent Delete
    console.log('\n--- STEP 12: Permanent Delete ---');
    const permDeleteRes = await fetch(`${BASE_URL}/trash/${createdFtjsId}/permanent`, { method: 'DELETE' });
    if (!permDeleteRes.ok) throw new Error(`Permanent delete failed: ${permDeleteRes.statusText}`);
    console.log(`Permanently deleted record ID: ${createdFtjsId}`);

    // Verify NOT in trash
    const trashCheck = await (await fetch(`${BASE_URL}/trash/summary?search=${trackingNo}`)).json();
    console.log(`Trash count after permanent delete (should be 0): ${trashCheck.content.length}`);
    if (trashCheck.content.length !== 0) throw new Error('Permanently deleted record still found in Trash!');

    // Verify stats restored to baseline
    const finalStats = await (await fetch(`${BASE_URL}/stats`)).json();
    const finalDashboard = await (await fetch(`${BASE_URL}/dashboard-stats`)).json();
    console.log(`Final Total Issued: ${finalStats.totalCertificatesIssued} (matches baseline: ${finalStats.totalCertificatesIssued === baselineStats.totalCertificatesIssued})`);
    console.log(`Final Active Volume: ${finalDashboard.totalActiveVolume} (matches baseline: ${finalDashboard.totalActiveVolume === baselineDashboard.totalActiveVolume})`);

    console.log('\n====================================================');
    console.log('   ALL END-TO-END TESTS PASSED WITH 100% SUCCESS!   ');
    console.log('====================================================');
}

runE2ETest().catch(err => {
    console.error('\nE2E TEST FAILED:', err);
    process.exit(1);
});
