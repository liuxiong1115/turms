<template>
    <content-template
        :name="name"
        :url="url"
        :record-key="recordKey"
        :query-key="queryKey"
        :deletion="deletion"
        :filters="filters"
        :action-groups="actionGroups"
        :table="table"
    />
</template>

<script>
import ContentTemplate from '../template/content-template';

export default {
    name: 'content-message-status-pane',
    components: {
        ContentTemplate
    },
    data() {
        return {
            name: 'message-status',
            url: this.$rs.apis.messageStatus,
            recordKey: 'key',
            queryKey: 'keys',
            deletion: {
                disabled: true
            },
            filters: [
                {
                    type: 'INPUT',
                    model: '',
                    name: 'messageIds',
                    placeholder: this.$t('messageId')
                },
                {
                    type: 'INPUT',
                    model: '',
                    name: 'recipientIds',
                    placeholder: this.$t('recipientId')
                },
                {
                    type: 'INPUT',
                    model: '',
                    name: 'senderIds',
                    placeholder: this.$t('senderId')
                },
                {
                    type: 'SELECT',
                    model: 'ALL',
                    name: 'areSystemMessages',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: this.$t('privateMessageAndGroupMessage')
                        },
                        {
                            id: false,
                            label: this.$t('userMessage')
                        },
                        {
                            id: true,
                            label: this.$t('systemMessage')
                        }]
                    }
                },
                {
                    type: 'SELECT',
                    model: 'ALL',
                    name: 'deliveryStatuses',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: this.$t('allDeliveryStatuses')
                        },
                        {
                            id: 'READY',
                            label: this.$t('ready')
                        },
                        {
                            id: 'SENDING',
                            label: this.$t('sending')
                        },
                        {
                            id: 'RECEIVED',
                            label: this.$t('received')
                        },
                        {
                            id: 'RECALLING',
                            label: this.$t('recalling')
                        },
                        {
                            id: 'RECALLED',
                            label: this.$t('recalled')
                        }]
                    }
                },
                {
                    type: 'DATE-RANGE',
                    model: [],
                    name: 'receptionDate'
                },
                {
                    type: 'DATE-RANGE',
                    model: [],
                    name: 'readDate'
                },
                {
                    type: 'DATE-RANGE',
                    model: [],
                    name: 'recallDate'
                }
            ],
            actionGroups: [
                [{
                    title: this.$t('updateSelectedMessageStatuses'),
                    type: 'UPDATE',
                    fields: [
                        {
                            type: 'DATE',
                            decorator: this.$validator.create('receptionDate')
                        },
                        {
                            type: 'DATE',
                            decorator: this.$validator.create('readDate')
                        },
                        {
                            type: 'DATE',
                            decorator: this.$validator.create('recallDate')
                        }
                    ]
                }]
            ],
            table: {
                columns: [
                    {
                        key: 'key.messageId',
                        width: '12.5%'
                    },
                    {
                        key: 'key.recipientId',
                        width: '12.5%'
                    },
                    {
                        title: this.$t('targetGroupId'),
                        key: 'groupId',
                        width: '12.5%'
                    },
                    {
                        key: 'isSystemMessage',
                        width: '7.5%'
                    },
                    {
                        key: 'senderId',
                        width: '10%'
                    },
                    {
                        key: 'deliveryStatus',
                        width: '7.5%'
                    },
                    {
                        key: 'receptionDate',
                        width: '12.5%'
                    },
                    {
                        key: 'readDate',
                        width: '12.5%'
                    },
                    {
                        key: 'recallDate',
                        width: '12.5%'
                    }
                ]}
        };
    }
};
</script>