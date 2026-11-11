package com.quare.blitzsplit.core.provider.analytics.screen

import com.quare.blitzsplit.core.model.AppDownloadSource
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.navigation.Route

// Why: one route of each kind, every id, code, name and key of it marked "secret", so a leak shows by its text.
internal val everyRouteKind: List<Route> = listOf(
    Route.Welcome,
    Route.ProfileSetup,
    Route.Restore,
    Route.Home,
    Route.Groups,
    Route.Search,
    Route.Profile,
    Route.CreateGroup,
    Route.EditProfile,
    Route.ProfilePhoto,
    Route.AppDownload(AppDownloadSource.HOME),
    Route.InAppUpdate(
        versionName = "2.1.0",
        storeUrl = "https://apps.apple.com/app/id1",
        source = UpdatePromptSource.STARTUP,
    ),
    Route.DeleteAccount,
    Route.NotificationSettings,
    Route.CategoryMap,
    Route.Invite(
        groupId = "secret-group",
        isNewGroup = true,
    ),
    Route.GroupDetail(
        groupId = "secret-group",
        joinedAs = "secret-name",
        tab = GroupTab.MEMBERS,
    ),
    Route.GroupStats(groupId = "secret-group"),
    Route.CategoryExpenses(
        groupId = "secret-group",
        categoryId = "secret-category",
        currency = GroupCurrency.BRL,
        periodStartMillis = 1L,
        periodEndMillis = 2L,
        isMonthPeriod = false,
    ),
    Route.EditGroup(groupId = "secret-group"),
    Route.GroupPhoto(groupId = "secret-group"),
    Route.DefaultSplit(groupId = "secret-group"),
    Route.SimplifyDebtsInfo(currency = GroupCurrency.BRL),
    Route.SplitBill(
        groupId = "secret-group",
        expenseId = "secret-expense",
        picksGroupFirst = true,
        scansReceipt = true,
        duplicatesExpenseId = null,
    ),
    Route.BillDetail(
        groupId = "secret-group",
        expenseId = "secret-expense",
        focusCommentId = "secret-comment",
    ),
    Route.ShareBill(
        groupId = "secret-group",
        expenseId = "secret-expense",
    ),
    Route.SharedBill(
        code = "secret-code",
        commentId = "secret-comment",
    ),
    Route.RegisterPayment(
        groupId = "secret-group",
        fromMemberId = "secret-from",
        toMemberId = "secret-to",
        currency = GroupCurrency.BRL,
        offersReceiverKeys = true,
    ),
    Route.PickPayment,
    Route.GlobalBalances(filter = BalancesFilter.OWES_YOU),
    Route.Charge(
        personKey = "secret-person",
        currency = GroupCurrency.BRL,
    ),
    Route.Contacts,
    Route.ContactProfile(personKey = "secret-person"),
    Route.ContactsAgenda,
    Route.ContactInvite(
        personKey = "secret-person",
        name = "secret-name",
    ),
    Route.Activity(
        personKey = "secret-person",
        showsBillsOnly = false,
    ),
    Route.SettleUp(
        personKey = "secret-person",
        currency = GroupCurrency.BRL,
    ),
    Route.JoinInvite(
        code = "secret-code",
        memberCode = "secret-member",
        inviterCode = "secret-inviter",
    ),
    Route.JoinWithLink(
        skippedCode = "secret-code",
        skippedMemberCode = "secret-member",
    ),
    Route.JoinPick(code = "secret-code"),
    Route.PaymentKey(
        method = PaymentMethod.PIX,
        keyId = "secret-key",
    ),
)
